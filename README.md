# Pinot
Pinot is a real-time distributed OLAP datastore, built to deliver scalable real-time analytics with low latency. It can ingest from batch data sources (such as Hadoop HDFS, Amazon S3, Azure ADLS, Google Cloud Storage) as well as stream data sources (such as Apache Kafka). 

This repo publishes the docker image and helm chart for [Apache Pinot](https://pinot.apache.org/).

## How do we use Pinot?
Hypertrace uses Pinot as underlying OLAP engine for realtime streaming ingestion of the traces, index them and serve the time-series and analytics queries from the hypertrace UI/dashboard.

| ![space-1.jpg](https://hypertrace-docs.s3.amazonaws.com/Hypertrace-architecture.png) | 
|:--:| 
| *Hypertrace Architecture* |


## Prerequisites
* Kubernetes 1.10+
* Helm 3.0+

## Docker Image
The docker image is published to [Docker Hub](https://hub.docker.com/r/hypertrace/pinot)

## Helm Chart Components
This chart will do the following:

* Create a Pinot cluster having multiple controllers, servers and brokers using [StatefulSets](http://kubernetes.io/docs/concepts/abstractions/controllers/statefulsets/).
* Create [PodDisruptionBudgets](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-disruption-budget/) for controllers, servers and brokers instances.
* Create [Headless Services](https://kubernetes.io/docs/concepts/services-networking/service/) to control the domain of the Pinot cluster.
* Create a Service configured to connect to the available Pinot controllers on the configured client port.
* Optionally apply a [Pod Anti-Affinity](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#inter-pod-affinity-and-anti-affinity-beta-feature) to spread the pinot cluster across nodes.
* Optionally start a JMX Exporter container inside Pinot pods.
* Optionally create a Prometheus ServiceMonitor for each enabled jmx exporter container.
* Optionally create a new storage class.

## Installing the Chart
You can install the chart with the release name `pinot` as below.

```console
$ helm upgrade pinot ./helm --install --namespace hypertrace
```

## Configuration
You can specify each parameter using the `--set key=value[,key=value]` argument to `helm install`.

Alternatively, a YAML file that specifies the values for the parameters can be provided while installing the chart. For example,

```console
$ helm upgrade my-release ./helm --install --namespace hypertrace -f values.yaml
```

## Default Values
- You can find all user-configurable settings, their defaults in [values.yaml](helm/values.yaml).
