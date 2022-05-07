# NewsCompare back-end project

* Install [maven](https://maven.apache.org/)
* Install [Java 8](http://www.oracle.com/technetwork/java/javase/overview/index.html)

### Docker
* Install [docker](https://www.docker.com/products/docker-desktop/)
* From a command line run `docker-compose up`

### Vagrant (deprecated)

A vagrant environment is included, this environment provides the postgresql server for local development.
It also allows testing of Ansible deployment and provisioning scripts locally.

* Install [vagrant](https://www.vagrantup.com/)
* Install [virtualbox](https://www.virtualbox.org/)
* From a command line run `vagrant plugin install vagrant-vbguest`
* From a command line run `vagrant up`

### VM options

Tweak the number of threads assigned to the common pool by setting the Java app to run with the flag 
`-Djava.util.concurrent.ForkJoinPool.common.parallelism=<NO_OF_THREADS>`, where NO_OF_THREADS can be reasonably set anywhere between 1-100 (depending on target machine).
