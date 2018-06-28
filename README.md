# gfe-test
Simple metric logging client & server for testing our GFE issue 

## Server

Servers run on `0.0.0.0:9002`

### JVM

```bash
cd scala
# choose one of the following
sbt runMain example.akkahttp.AkkaHttpServer
sbt runMain example.jerseygrizzly.JerseyGrizzlyServer
sbt runMain example.nettypure.NettyServer
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

## Monitoring

```bash
./watchconnections.sh
```

## Results

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

## Slow server

we introduce 200ms delay on the response

Client conn | Client req/sec | AkkaHttp conn
---         | ---            | ---
0           | 0              | 0
1           |              | 
2           |              | 
4           |             | 
16          |             | 
64          |            |     


### Observations

* The number of connections on serverside goes up fast
* Observed socket polling, every 5 sec we get 20 connections from upstream
* All requests returned 200 which is good
* When using Node.js as server the connections drop fast (3sec) when the client disconnects, with akka and go this takes a much longer time (10 min?) 