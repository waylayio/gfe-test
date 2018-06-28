# gfe-test
Simple metric logging client & server for testing our GFE issue 

## Server

Servers run on `0.0.0.0:9002`

### JVM

```bash
cd scala
# choose one of the following
sbt "runMain example.akkahttp.AkkaHttpServer"
sbt "runMain example.jerseygrizzly.JerseyGrizzlyServer"
sbt "runMain example.nettypure.NettyServer"
```

### Node.js

```bash
cd noden
pm start
```

### Go

```bash
cd go
go run main.go
```

## Client

```bash
cd scala
sbt runMain example.TestClient
```

modify source to tweak delay / client connection pool

## Monitoring

```bash
./watchconnections.sh
```

## Results

### Fast server

Run a single server with google ingress controller in front (default config)
Run a single client with connection pool (from our office)

We are mainly investigating the number of open sockets on the server compared to the number of open sockets on the client

Client conn | Client req/sec | AkkaHttp conn | Go conn  | Node conn |
---         | ---            | ---           | ---      | ---       |
0           | 0              | 0             | 0        | 0         |
1           | 35             | 32            | 32       | 30        |
2           | 74             | 58            | 59       | 58        |
4           | 150            | 116           | 142      | 94        |
16          | 590            | 210           | 250      | 170       |
64          | 2250           | 360           | 360      | 270       |

### Slow server

we introduce a delay on the response and always work with 64 client connections

Server delay (ms) | Client req/sec | AkkaHttp conn | AkkaHttp active req
---               | ---            | ---           | ---
0                 | 2250           | 348           | 4
1                 | 1190           | 460           | 64
2                 | 1160           | 460           | 64
20                | 980            | 462           | 64
100               | 448            | 432           | 64
200               | 260            | 428           | 64
1000              | 64             | 310           | 64

probably the low delays can not be respected by the server

### High client connection count

Akka http limits the number of server connections to 1024 by default, we try to find a as high as possible client setting until we get 502 errors

Client set up to open a max of 512 connections, we get 8000 req/sec and the load balancer has only 680 connections open to the server and akka reports 4 active requests


### Observations

* The number of connections on serverside goes up fast
* Observed socket polling, every 5 sec we get 20 connections from upstream
* All requests returned 200 which is good
* When using Node.js as server the connections drop fast (3sec) when the client disconnects, with akka 1 min and go 10 min.


__So why do we on this other system see 1024+ connections to a akka http server if there are only 50 req/sec?__