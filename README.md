# Pinot
Pinot is a real-time distributed OLAP datastore, built to deliver scalable real-time analytics with low latency. It can ingest from batch data sources (such as Hadoop HDFS, Amazon S3, Azure ADLS, Google Cloud Storage) as well as stream data sources (such as Apache Kafka). 

This repo publishes the docker image and helm chart for [Apache Pinot](https://pinot.apache.org/).

## Description
Hypertrace uses Pinot as underlying OLAP engine for realtime streaming ingestion of the traces, index them and serve the time-series and analytics queries from the hypertrace UI/dashboard.

| ![space-1.jpg](https://hypertrace-docs.s3.amazonaws.com/HT-architecture.png) | 
|:--:| 
| *Hypertrace Architecture* |


## Building Locally
To build Pinot image locally, run:

```
./gradlew dockerBuildImages
```

`Note:` 
- docker-compose uses `pinot-servicemanager` image so you have to build it from that folder in case you are working on that one. 
- To read more about installing and configuring helm chart refer [BUILD.md](https://github.com/hypertrace/pinot/BUILD.md).

## Testing
You can test the image you built after modification by running docker-compose or helm setup. 

### docker-compose
Change the tag for `pinot-servicemanager` from `:main` to `:test` in [docker-compose file](https://github.com/hypertrace/hypertrace/blob/main/docker/docker-compose.yml) like this.

```yaml
  pinot:
    image: hypertrace/pinot-servicemanager:test
    container_name: pinot
    ...
```

and then run `docker-compose up` to test the setup.



