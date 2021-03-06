# wkhtmltopdf as a web service

A dockerized webservice written in [Spring Boot / Java](https://projects.spring.io/spring-boot/) that uses [wkhtmltopdf](http://wkhtmltopdf.org/) to convert HTML into documents (images or pdf files).
Supports **wkhtmltoimage** as well for generation of .png and .jpg formats.

## Usage

The service listens on port 3000 for POST requests on the root path (`/`). 
The body should contain a JSON-encoded object containing the following parameters:

- **url**: The URL of the page to convert starting with `http://` or `https://`
- **options**: A list of key-value arguments that are passed on to the appropriate `wkhtmltopdf` binary. Boolean values are interpreted as flag arguments (e.g.: `--greyscale`). Repeatable options are nested. (see the examples and wkhtml docs for **--allow","--bypass-proxy-for","--cookie","--custom-header","--post","--post-file","--run-script","--replace** ).
- **cmdline**: An alternative form of presenting arguments in a string with comma separated arguments just as listed in the [wkhtmltopdf docs](https://wkhtmltopdf.org/usage/wkhtmltopdf.txt) except for the executable binary filename and the output [-] parameter.

The type of document to generate can be either `jpg`, `png` or `pdf`. Defauts to `pdf` if not specified. Depending on the output type the appropriate binary is called.
**Example:** posting the following JSON:

```
{
  "url": "http://www.google.com",
  "options": {
    "--margin-bottom": "1cm",
    "--orientation": "Landscape",
    "--grayscale": true,
    "--cookie": {
        "foo": "bar",
        "baz": "foo"
    }
  }
}
```

will have the effect of the following command-line being executed on the server:

```
/usr/local/bin/wkhtmltopdf --margin-bottom 1cm --orientation Landscape --grayscale --cookie foo bar --cookie baz foo http://www.google.com -
```

and is just the same as:

```
{
  "url": "http://www.google.com",
  "cmdline" : "--margin-bottom, 1cm, --orientation, landscape, --grayscale, --cookie, foo, bar, --cookie, baz, foo"
}
```

See more [Examples](Examples.md)

The `-` at the end of the command-line is so that the document contents are redirected to stdout so we can in turn redirect it to the web server's response stream.

When using `jpg` or `png` output, the set of options you can pass are actually more limited. Please check [wkhtmltopdf usage docs](http://wkhtmltopdf.org/docs.html) or rather just use `wkhtmltopdf --help` or `wkhtmltoimage --help` to get help on the available command-line arguments.
Image output can be achieved with the --format: png or --format: jpg options.

## Response status

The service returns these http status codes:

- 200: `Ok` with response `content-type` `application/pdf` (or `image/jpeg` or `ìmage/png`)
- 400: `Bad request` in case of json request parsing errors. Response body contains error information `content-type` `text/html` from the parser.
- 404: `Not found` if the service path is incorrect or
- 405: `Not allowed` if the service method is incorrect
- 500:  `Internal server error` in case of wkhtmltopdf exits with return code other than 0. Response body contains the errorStream generated by wkhtmltopdf with `content-type` `text/html`.

## Build instructions

[Build instructions](Build.md)

## Release history

0.0.3 Improved wkhtml error logging

## TODO

Sorry - we have no testcases yet :(

## License

This code is released under the [Apache License](https://opensource.org/licenses/Apache-2.0).
