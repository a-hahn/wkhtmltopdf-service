
Google homepage

```
curl -H "Content-Type: application/json" -X POST -d '{
"url" : "www.google.com"
}'  http://localhost:3000 -w "%{http_code}" -o google.pdf
```

Example homepage with jwt authorization

```
curl -H "Content-Type: application/json" -X POST -d '{
    "url" : "http://wwww.example.com/some/path",
    "options" : {
        "-s" : "A4",
        "-l" : true,
        "--print-media-type" : "true",
        "--javascript-delay" : "10000",
        "--custom-header-propagation" : "true",
        "--custom-header" : {
            "Authorization" : "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtdGIiLCJleHAiOjE1MTk4NDI1OTB9.eAhS3PnhIEJmBLnSzOtAj1Hg29TzNEe07TAkdjKF5vwUO7iIzM5ofO0y2n_4JLI-2qU8wpCmydgPWQUxCh0frA"
        }
    }
}'  http://localhost:3000 -w "%{http_code}" -o app.pdf
```

Google homepage as image

```
curl -H "Content-Type: application/json" -X POST -d '{
"url" : "www.google.com",
"cmdline" : "--format, png,--width,1000,--height,1000"
}'  http://localhost:3000 -w "%{http_code}" -o google.png
```

Example homepage with cookie

```
curl -H "Content-Type: application/json" -X POST -d '{
    "url" : "http://www.example.com/some/path,
    "cmdline" : "-s, A4, -l, --javascript-delay, 6000, --custom-header-propagation",
    "options" : {
        "--cookie" : {
            "JSESSIONID" : "46FE1BDC0CC4F8E5DAE0D7C4EB58EC49"
        }
    }
}'  http://localhost:3000 -w "%{http_code}" -o test.pdf
```

Show details about the installed wkhtmltopdf engine

```
curl -H "Content-Type: application/json" -X POST -d '{
"url" : "www.google.com", "cmdline" : "-h"
}'  http://localhost:3000 -w "%{http_code}"
```
