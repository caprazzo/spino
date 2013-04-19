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
		- [Endpoint](#endpoint)
		- [Spino Node](#spino-node)
	- [Usage](#usage)
		- [Starting Spino](#starting-spino)
		- [Activating a endpoint](#activating-a-endpoint)
		- [Listing active services](#listing-active-services)
		- [Deactivating an endpoint](#deactivating-an-endpoint)
		- [Shutting down Spino](#shutting-down-spino)
		- [Maven](#maven)

## Concepts

### Service

A `Service` is just a string that uniquely identifies an HTTP interface. 

For example `auth-api-v1`.

### Endpoint

An `Endpoint` is an instance of a `Service`, and is identified by a string and an address. 

For example, `Endpoint("auth-api-v1", "http://auth-host-0:8080")`

An `Endpoint` is associated with a `Spino Node`.

Multiple endpoints can exist for the same Service, provided that have a different Address.

### Spino Node

A `Spino Node` is any java program that joins the `Spino Cluster`.

A `Spino Node` can notify the cluster that an Endpoint has
become available or unavailable, and list service endpoints.

If a Node goes down or becomes unreachable, any Endpoint it
declared available, automatically becomes unavailable.

Obviously this works best if a Node is also an Endpoint itself
(for example, a java program that embeds a jetty server is an ideal Spino Node).

Spino only checks if nodes are up or down and DOES NOT check
if services are in fact working. Each node is responsible
for activating services that actually work and deactivate
those that do not.

## Usage

### Starting Spino

To join a Spino cluster, provide the address of other known nodes
(one is enough, but it must be up for the join to succeed)
```java
Spino spino = new Spino();
spino.start("192.168.0.2", "192.168.0.3");
```

(If you are in a multicast environment, you don't need to specify any other node). Just use start()

### Activating a endpoint

When a node knows that a service is ready, it can activate it using

```java
spino.activateServiceEndpoint("database-v1", "http://db-0:8001");
```

### Listing active services

Any node can list all the active services in the cluster:

```java
for (spino.Service endpoint : spino.getServiceEndpoints("database-v1")) {
    System.out.println("db available at: " + endpoint.getAddress());
}
```
### Deactivating an endpoint

A node can withdraw any endpoint at any time.
```java
spino.deactivateServiceEndpoint("database-v1", "http://db-0:8001");
```

### Shutting down Spino

A terminated node will automatically be removed from the cluster,
but it's good practice to explicitly remove a node before shutdown
or if it's under maintenance.

When a node is shut down, all its services are deactivated.

```java
spino.shutdown()
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














