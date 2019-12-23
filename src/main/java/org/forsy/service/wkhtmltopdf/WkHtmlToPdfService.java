package org.forsy.service.wkhtmltopdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


/**
 * @author Andreas Hahn
 *
 * Wrapper for the wkhtmltopdf (and wkhtmltoimage) open source pdf generator
 * To be installed on Linux OS
 * @see "https://wkhtmltopdf.org/"
 *
 * Can be used with Docker. Follow the instructions in Build.md
 *
 */
@Controller
@SpringBootApplication
public class WkHtmlToPdfService {

    protected final Log logger = LogFactory.getLog(getClass());

    private final String WK_HTML_TO_PDF = "wkhtmltopdf";

    private final String WK_HTML_TO_IMAGE = "wkhtmltoimage";

    private String pdfExe;

    private String imageExe;

    private String[] paths = {"/usr/local/bin/","/usr/bin/"};

    private String output = "-";	// stdout see doc wkhtmltopdf

    private long serNo = 0;

    @RequestMapping(method = RequestMethod.POST, consumes = APPLICATION_JSON_VALUE)
    public void createPdf(HttpServletRequest request, HttpServletResponse response,
                          @RequestBody LinkedHashMap<String, Object> parms) {

        long serialNum = serNo++;
        Integer copyCount = 0;
        File tmpFile = null;

        String url = (String) parms.get("url");
        String cmdline = (String) parms.get("cmdline");
        List<String> cmdParms = new ArrayList<String>();
        // NOTE the binary (wkhtmtopdf or wkhtmltoimage) will be set later by setWkHtmlMode
        cmdParms.addAll(getCommandLine(cmdline));
        cmdParms.addAll(getJsonParms(parms));
        cmdParms.add(url);
        cmdParms.add(output);
        response.setContentType(setWkHtmlMode(cmdParms));

        long startTime = System.currentTimeMillis();
        long byteCount = 0;

        ProcessBuilder pb = new ProcessBuilder(cmdParms);
        Process process = null;
        StringWriter errorWriter = new StringWriter();
        byte[] responseBuffer;
        try {
            serialNum++;
            logger.info("PDF #" + serialNum + " with '" + url + "'");
            logger.debug("PDF #" +serialNum + " cmd: " +  cmdParms.toString());
            process = pb.start();
            ErrorGobbler errorGobbler = new ErrorGobbler(process.getErrorStream(), errorWriter, serialNum);
            errorGobbler.start();
            // As the errorStream is written async we need to use a buffer, else response is commited and we can't write header anymore
            responseBuffer = FileCopyUtils.copyToByteArray(process.getInputStream());
            byteCount = responseBuffer.length;

            int exitCode = process.waitFor();
            if (exitCode == 1) {
                logger.warn("PDF #" + serialNum + " terminated with error code '1' which is undocumented but probably ok, cmd: '" +  cmdParms.toString() + "'");
            }
            if (exitCode > 1) {
                throw new WkHtmlToPdfException(exitCode, serialNum, errorWriter, cmdParms.toString());
            }
            errorGobbler.interrupt();
            FileCopyUtils.copy(responseBuffer,response.getOutputStream());
        } catch (IOException | InterruptedException e) {
            logger.error("PDF #" + serialNum + " terminated with internal errors, cmd: '" +  cmdParms.toString() + "'", e);
            throw new WkHtmlToPdfException(0, serialNum, errorWriter, cmdParms.toString());
        } catch (WkHtmlToPdfException e) {
            // This can and will happen and must be returned to the caller with
            logger.error("PDF #" + serialNum + " terminated with wkhtmltopdf exitCode " + e.wkhtmltopdfMsg + ", cmd: '" +  cmdParms.toString() + "'", e);
            throw e;
        } finally {
            try {process.getInputStream().close();} catch (NullPointerException | IOException ignore) {};
            try {process.getErrorStream().close();} catch (NullPointerException | IOException ignore) {};
            long duration = System.currentTimeMillis() - startTime;
            logger.info("PDF #" + serialNum + " done, bytes: " + byteCount + ", msecs: " + duration);
        }
    }

    /**
     * Set the program type (pdf or image) depending on the --format parameter
     *
     * @param cmdParms
     * @return content type
     */
    private String setWkHtmlMode(List<String> cmdParms) {
        String mediaType = "application/pdf";
        String cmd = pdfExe;
        int idx = cmdParms.indexOf("--format");
        if (idx >= 0 && imageExe != null) {
            String format = cmdParms.get(idx+1);
            switch (format.trim().toLowerCase()) {
                case "png" : {
                    mediaType = "image/png";
                    cmd = imageExe;
                    break;
                }
                case "jpg" : {
                    mediaType = "image/jpeg";
                    cmd = imageExe;
                    break;
                }
            }
        }
        cmdParms.add(0,cmd);
        return mediaType;
    }

    /**
     * Error handler to prepare the error response
     * @param res
     * @param errStatus
     * @param ex
     */
    public void prepareErrorResponse(HttpServletResponse res, int errStatus, Throwable ex) {
        if (res.isCommitted()) {
            logger.error("Response already commited, Cannot return error '" + errStatus + "' to caller", ex);
        } else {
            res.reset();
            res.setContentType("text/plain;charset=UTF-8");
            res.setStatus(errStatus);
        }
    }

    /**
     * Write error response to client for errors during JSON object parsing
     * @param res
     * @param ex
     */
    @ExceptionHandler(HttpMessageConversionException.class)
    public void handleJsonException(HttpServletResponse res, Throwable ex){
        prepareErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, ex);
        HttpMessageConversionException msgEx = (HttpMessageConversionException) ex;
        try {
            PrintWriter writer = res.getWriter();
            writer.println("Client request has errors");
            writer.println(msgEx.getMessage());
        } catch (IOException ignore) {  } // ignore errors as client is probably closed already
    }

    /**
     * Write error response to client for errors (errorStream)
     * that are reported from the wkhtmltopdf service
     * @param res
     * @param ex
     */
    @ExceptionHandler(WkHtmlToPdfException.class)
    public void handleBackendException(HttpServletResponse res, Throwable ex){
        prepareErrorResponse(res,HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        ((WkHtmlToPdfException) ex).printErrorResponse(res);
    }

    /**
     * Write any unspecific errors back to the client.
     * This is questionable and maybe we shouldnt report the whole stacktrace back to caller !?
     * @param res
     * @param ex
     */
    @ExceptionHandler(Exception.class)
    public void handle(HttpServletResponse res, Throwable ex){
        prepareErrorResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        if (!res.isCommitted()) {
            try {
                ex.printStackTrace(res.getWriter());
            } catch (IOException e) {
                logger.error("Error while writing error repsonse", ex);
            }
        }
    }

    /**
     * Add all arguments presented as json object into a comma separated List
     * that is suitable for the ProcessBuilder
     * @param parms
     * @return
     */
    private List<String> getJsonParms( LinkedHashMap<String, Object> parms) {
        List<String> cmdList = new ArrayList<>();
        Map<String,Object> args = (Map<String,Object>) parms.get("options");
        addOptions(cmdList,  args,"");
        return cmdList;
    }

    /**
     * Add all commandline arguments to the argument list
     * Take care to not remove whitespace from arguments such as "Bearer blablablabearer"
     * as this kills authorization and other headers
     * @param cmdline
     * @return
     */
    private List<String> getCommandLine(String cmdline) {
        String[] cmds = StringUtils.delimitedListToStringArray(cmdline, ",");
        for (String cmd : cmds) {
            cmd = cmd.trim();
        }
        return Arrays.asList(cmds);
    }

    /**
     * Detect json values to be treated as a single argument
     * @param arg
     * @return
     */
    private boolean isSingleArgument(String arg) {
        String[]bools = {"true","false"};	// add more values here if appropriate
        if (!StringUtils.hasText(arg)) {return false;};
        String testArg = arg.trim().toLowerCase();
        for (String test : bools) {
            if (test.equals(arg)) {return true;};
        }
        return false;
    }

    /**
     *
     * @param argList
     * @param parms
     * @param key
     */
    private void addOptions(List<String> argList, Map<String,Object> parms, String key) {
        if (null==parms) {return; };
        for (Map.Entry<String,Object> keyValue : parms.entrySet()) {
            if (StringUtils.hasText(key)) {
                argList.add(key.trim());
            }
            argList.add(keyValue.getKey().trim());
            Object val = keyValue.getValue();
            if (val instanceof String) {
                String value = (String) val;
                if (!isSingleArgument(value)) {
                    argList.add(value.trim());
                }
            } else if (val instanceof Map) {
                addOptions(argList,(Map<String,Object>) val, key);
            }
        }
    }

    /**
     * Depending on the installation wkhtmltopdf might be installed in different dirs
     */
    @PostConstruct
    public void setCommandPath() {
        String exe = "";
        for (String path : paths) {
            exe = path + WK_HTML_TO_PDF;
            if (new File(exe).exists()) {
                pdfExe = exe;
                break;
            }
        }
        for (String path : paths) {
            exe = path + WK_HTML_TO_IMAGE;
            if (new File(exe).exists()) {
                imageExe = exe;
                break;
            }
        }
        if (null == pdfExe) {
            throw new IllegalArgumentException("PDF Service configuration invalid");
        }
    }

    /**
     * Background process for wkhtmltopdf error output
     * to avoid deadlocks just in case we have to digest huge error output.
     * Output larger than os buffer size might block the wkhtmltopdf process.
     * For that reason we need to handle errors in this separate thread.
     */
    public class ErrorGobbler extends Thread{
        InputStream is;
        PrintWriter writer;
        long serialNum;

        public ErrorGobbler(InputStream is, Writer writer, long serialNum)
        {
            this.is = is;
            this.writer = new PrintWriter(writer,true);
            this.serialNum = serialNum;
        }

        public void run()
        {
            try
            {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line=null;
                while ( (line = br.readLine()) != null) {
                    String errStr = "PDF #" + serialNum + " " + line;
                    if (line.startsWith("Exit with code")) {
                        logger.warn(errStr);
                    } else {
                        logger.debug(errStr);
                    }
                }
                while ( (line = br.readLine()) != null)
                    writer.println(line);
            } catch (Exception ioe) {
                writer.println("PDF Service I/O Exception during error stream read");
            }
        }

    }

    /**
     * We throw this exception for all errors related to the wkhtmltopdf service call
     */
    public class WkHtmlToPdfException extends RuntimeException {
        private int errCode;
        private long serialNum;
        private StringWriter wkhtmltopdfMsg;
        private String commandString;

        WkHtmlToPdfException(int errorCode, long serialNum, StringWriter wkhtmltopdfMsg, String command) {
            super("WkHtmlToPdf service exception occurred");
            this.errCode = errorCode;
            this.serialNum = serialNum;
            this.commandString = command;
            this.wkhtmltopdfMsg = wkhtmltopdfMsg;
        }

        public void printErrorResponse(HttpServletResponse res) {
            if (!res.isCommitted()) {
                try {
                    PrintWriter writer = res.getWriter();
                    writer.println("WkHtmlToPdf error " + String.valueOf(this.errCode) + " occured" );
                    writer.println("PDF # " + serialNum + " Arguments: " + this.commandString);
                    writer.println(this.wkhtmltopdfMsg.toString());
                } catch (IOException ignore) {
                    // should we log this ? Probably connection to client is terminated so theres no point in logging that
                }
            }
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(WkHtmlToPdfService.class, args);
    }


}
