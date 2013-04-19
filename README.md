Spino - simple clustering for java HTTP services
================================================

Spino is a simple tool to create clusters of java HTTP services.

A endpoint is defined by a name and url. In a cluster, a endpoint
will have several urls.

Any java program can become a Node in Spino cluster. Any node can
activate and deactivate endpoint instances, and query the cluster
about availability and location of services.

Spino uses Hazelcast to maintain an shared, up-to date view of
services, their URLs and availability. If a Node goes down or becomes
unreachable, it's active services are marked as down.

Spino only checks if nodes are up or down and DOES NOT check
if services are in fact working. Each node is responsible
for activating services that actually work and deactivate
those that do not.

## Usage

Spino is a Java library and you'll need to write some simple
code to use it in your services.

### Maven

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

### Example System

Here is a simple, somewhat realistic imaginary system:
 - The Database Service (DB): a java web endpoint that offers up some data.
 - The Auth Service (AUTH): another java web endpoint that offers authentication services.  AUTH uses DB
 - The Webapp (WEB): a java web application that does lots of things WEB uses both AUTH and DB

And here is our imaginary system deployed in production:

    +----------+-------------+--------------------+
    | Service  |    Name     |        Url         |
    +----------+-------------+--------------------+
    | Database | database-v1 | http://db-0:8001   |
    | Database | database-v1 | http://db-1:8001   |
    | Auth     | auth-v1     | http://auth-1:8002 |
    | Auth     | auth-v1     | http://auth-2:8002 |
    | Web App  | webapp      | http://webapp.com  |
    | Web App  | webapp      | http://webapp.com  |
    +----------+-------------+--------------------+

### Starting Spino

    Spino spino = new Spino();
    spino.start();

### Activating a endpoint

When a endpoint instance (a 'node') is ready to accept connections, it should let the rest of the cluster
know where it's http endpoint is located.

A node can activate any number of endpoint, but each endpoint
name must be unique across the cluster.  If there are multiple versions of the same endpoint, be sure
to give them different names.

    // Here is what Database Service should do, when it's ready to accept connections:
    // (the actual hostname and port should come from configuration...)
    spino.activateLocalService("database-v1", "http://db-0:8001");

    // And this is what AUTH should do when it's ready:
    // (the actual hostname and port should come from configuration...)
    spino.activateLocalService("auth-v1", "http://auth-0:8002");

### Listing active services

Any node can list all the actives services in the cluster

    // An Auth instance asks what Database instances are available
    List<Spino.Service> serviceEndpoints = Spino.getServiceEndpoints("database-v1");

    for (Spino.Service endpoint : serviceEndpoints) {
        System.out.println("db available at: " + endpoint.getAddress());
    }

### Deactivating a endpoint

A node can withdraw any endpoint at any time.

    // This is Database deactivating the endpoint on port 8001
    spino.deactivateLocalService("database-v1", "http://db-0:8001");

A endpoint is responsible for deactivating any endpoint that is faulty.

### Shutting down Spino

A terminated node will automatically be removed from the cluster,
but it's good practice to explicitly remove a node before shutdown
or if it's under maintenance.

When a node is shut down, all its services are deactivated.

    // remove this node from the cluster
    spino.shutdown()

### Configuring Hazelcast

Spino uses hazelcast to maintain the endpoint map and detect failed nodes.

By default, Hazelcast will try to find other cluster members using multicast.

If that does not work for you, please consult the Hazelcast Manual:
http://www.hazelcast.com/docs/2.5/manual/multi_html/ch12.html












