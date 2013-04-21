Spino - simple clustering for java HTTP services
================================================

Spino is a simple java library that helps with clustering HTTP services.

Use Spino to implement HTTP redundancy without HTTP proxies.
Each Spino Node always knows all the available http services,
so it can make calls directly to active servers.

**Table of Contents**

- [Spino - simple clustering for java HTTP services](#spino---simple-clustering-for-java-http-services)
	- [Concepts](#concepts)
		- [Service](#service)
		- [Location](#location)
		- [Spino Node](#spino-node)
	- [Usage](#usage)
		- [Configuring and Starting Spino](#configuring-and-starting-spino)
		- [Activating a Location](#activating-a-location)
		- [Listing active services](#listing-active-services)
		- [Deactivating an serviceInstance](#deactivating-an-serviceinstance)
		- [Shutting down Spino](#shutting-down-spino)
		- [Maven](#maven)
	- [Using With Apache HttpClient](#using-with-apache-httpclient)
	- [Using with httpclient-failover](#using-with-httpclient-failover)

## Concepts

### Service

A `Service` is just a string that uniquely identifies an HTTP interface. 

For example `auth-api-v1`.

### Location

An `Location` is an instance of a `Service`, and is identified by a string and an address. 

For example, `Location("auth-api-v1", "http://auth-host-0:8080")`

An `Location` is associated with a `Spino Node`.

Multiple locations can exist for the same Service.

### Spino Node

A `Spino Node` is any java program that joins the `Spino Cluster`.

A `Spino Node` can notify the cluster that a Location has
become available or unavailable, and list service locations.

If a Node goes down or becomes unreachable, any Location it
declared available, automatically becomes unavailable.

Obviously this works best if a Node is also an Location itself
(for example, a java program that embeds a jetty server is an ideal Spino Node).

Spino only checks if nodes are up or down and DOES NOT check
if services are in fact working. Each node is responsible
for activating services that actually work and deactivate
those that do not.

## Usage

### Configuring and Starting Spino

```java
Spino.start();
```

The above only work if the nodes are in a multicast environment. If this is not
true for you, you can specify other nodes addresses by

```java
Spino.start("192.168.0.2", ""192.168.0.3")
```

Spino uses [Hazelcast]() to maintain the cluster. For more advanced configuration options
please consult the [Hazelcast Manual](http://www.hazelcast.com/docs/2.5/manual/multi_html/ch12.html)

### Activating a Location

When a node knows that a service is ready, it can activate it using

```java
Spino.activateLocation("database-v1", "http://db-0:8001");
```

### Listing active services

Any node can list all the active services in the cluster:

```java
for (URL location : Spino.getLocations("database-v1")) {
    System.out.println("database-v1 available at: " + location);
}
```
### Deactivating an serviceInstance

A node can withdraw any serviceInstance at any time.
```java
Spino.deactivateLocation("database-v1", "http://db-0:8001");
```

### Shutting down Spino

A terminated node will automatically be removed from the cluster,
but it's good practice to explicitly remove a node before shutdown
or if it's under maintenance.

When a node is shut down, all its services are deactivated.

```java
Spino.shutdown()
```

### Maven
```xml
<repositories>
    <repository>
        <id>mcaprari-releases</id>
        <url>https://github.com/mcaprari/mcaprari-maven-repo/raw/master/releases</url>
    </repository>
    <repository>
        <id>mcaprari-snapshots</id>
        <url>https://github.com/mcaprari/mcaprari-maven-repo/raw/master/snapshots</url>
    </repository>
</repositories>

<dependency>
    <groupId>spino</groupId>
    <artifactId>spino-core</artifactId>
    <version>1.0</version>
</dependency>
```

## Using With Apache HttpClient

`spino-httpclient` makes it easier to use Spino with [Apache HttpClient](http://hc.apache.org/httpcomponents-client-ga/index.html)

Add `spino-httpclient` to your dependencies:

```xml
<dependency>
    <groupId>spino</groupId>
    <artifactId>spino-httpclient</artifactId>
    <version>[1.1,)</version>
</dependency>
```

Crude example of retrying the same http call over multiple Spino Nodes:
(if this is what you are after, see `HttpClient-Failover` below)

```java
Spino.start();

// every time provider.iterator() is called,
// it returns a new iterator over the most recent list of active locations:
Iterable<HttpHost> provider = SpinoHttpHostProvider.ofService("my-service");

HttpClient client = new DefaultHttpClient();
HttpGet request = new HttpGet("/index.html");

Iterator<HttpHost> hostIterator = provider.iterator();
while(hostIterator.hasNext()) {
    HttpHost target =  hostIterator.next();
    try {
        HttpResponse response = client.execute(target, request);
        // do something with your response
        break;
    }
    catch(IOException ex) {
        System.err.println("WARNING: Failed to execute " + request + " on " + target);
        if (hostIterator.hasNext()) {
            System.err.println("WARNING: Trying on next host...");
        }
        else {
            break;
        }
    }
}
```

## Using with httpclient-failover

[Httpclient-Failover](github.com/mcaprari/httpclient-failover) is an http client that allows to failover over multiple hosts,
and it's very easy to use with spino.

Add dependencies to `spino-httpclient` and `httpclient-failover`

```xml
<dependency>
    <groupId>spino</groupId>
    <artifactId>spino-httpclient</artifactId>
    <version>1.0</version>
</dependency>

<dependency>
    <groupId>httpfailover</groupId>
    <artifactId>httpclient-failover</artifactId>
    <version>[1.1,)</version>
</dependency>
```

Then simply use a `SpinoHttpHostProvider` with  `httpClient.execute()`

```java
Spino.start();

Iterable<HttpHost> provider = SpinoHttpHostProvider.ofService("my-service");

FailoverHttpClient httpClient = new FailoverHttpClient();

HttpGet request = new HttpGet("/index.html");

// this will try the request on all hosts
HttpResponse httpResponse = httpClient.execute(provider, request);
```












