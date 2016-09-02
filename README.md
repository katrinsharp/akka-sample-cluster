
## Creating Docker images

```
docker build -t activator -f Dockerfile-activator .

docker build -t sources -f Dockerfile-sources .
```

`activator` is a base image for `sources`. It need to build only once, then evert time that sources change, rebuild `sources` only.


## Running locally in Docker on mac:

```
docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_THIS_PORT=2551" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" -p "2551:2551"  --name akka-sample-backend-1 sources ./bin/activator "runMain sample.cluster.factorial.FactorialBackend"

docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_THIS_PORT=2552" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" -p "2552:2552"  --name akka-sample-backend-2 sources ./bin/activator "runMain sample.cluster.factorial.FactorialBackend"

docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" --name akka-sample-frontend sources ./bin/activator "runMain sample.cluster.factorial.FactorialFrontend"
```

Substitute `192.168.99.100` with IP of your Docker machine (`docker-machine env`).

## Google Container Engine (GKE)

### Pushing images to GKE: [doc](https://cloud.google.com/container-registry/docs/pushing)

Note: Don't forget to change images in yaml files to what you've pushed to GKE registry.

### Creating GKE cluster: [doc](https://cloud.google.com/container-engine/docs/clusters/operations)

### Credentials for K8S dashboard
`gcloud container clusters akka-cluster-3 | grep username`
`gcloud container clusters akka-cluster-3 | grep password`

Note: Don't forget to change to your cluster's name

### K8S dashboard link
`kubectl cluster-info`

### Deployments: *.yaml files in the root directory
