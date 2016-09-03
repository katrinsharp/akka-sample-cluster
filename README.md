
## Creating Docker images

```
docker build -t activator -f Dockerfile-activator .

docker build -t sources -f Dockerfile-sources .
```

`activator` is a base image for `sources`. It need to build only once, then evert time that sources change, rebuild `sources` only.


## Running locally in Docker on mac:

```
cd akka-sample-cluster

docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_THIS_PORT=2551" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" -p "2551:2551"  --name akka-sample-backend-1 sources ./bin/activator "runMain sample.cluster.factorial.FactorialBackend"

docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_THIS_PORT=2552" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" -p "2552:2552"  --name akka-sample-backend-2 sources ./bin/activator "runMain sample.cluster.factorial.FactorialBackend"

docker run -d -it -e "THIS_IP=192.168.99.100" -e "AKKA_SAMPLE_SEED_IP_1=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_1=2551" -e "AKKA_SAMPLE_SEED_IP_2=192.168.99.100" -e "AKKA_SAMPLE_SEED_PORT_2=2552" --name akka-sample-frontend sources ./bin/activator "runMain sample.cluster.factorial.FactorialFrontend"
```

Substitute `192.168.99.100` with IP of your Docker machine (`docker-machine env`).

## Google Container Engine (GKE)

### Pushing images to GKE: [doc](https://cloud.google.com/container-registry/docs/pushing)

In my case:
```docker tag sources gcr.io/test1-1384/sources```
```gcloud docker push gcr.io/test1-1384/sources```


Note: Don't forget to change image names in yaml files to what you're really pushing to GKE registry.

### Creating GKE cluster: [doc](https://cloud.google.com/container-engine/docs/clusters/operations)

### Credentials for K8S dashboard
`gcloud container clusters akka-cluster-3 | grep username`
`gcloud container clusters akka-cluster-3 | grep password`

Note: Don't forget to change to your cluster's name

### K8S dashboard link
`kubectl cluster-info`

### Deployments: *.yaml files in the root directory should be copied to Google Cloud shell

```kubectl create -f discovery-svc.yaml```

```kubectl create -f backend-seed.yaml```

```kubectl create -f frontend.yaml```

Note: the deployments are set to have replicas=0

To start up the cluster:

```kubectl scale --replicas=2 deployment/cluster-backend-seed```

```kubectl scale --replicas=1 deployment/cluster-frontend```

### Seed nodes and dynamic IPs

In order to be able to join for node in Akka Cluster, it should know cluster seed nodes IPs and ports. When ports could be fixed, IPs in K8S are dynamic.

To provide dynamic discovery for seed nodes you can use etcd, ConstructR or K8S built-in headless service functionality. This project uses headless service approach. The idea of this approach is to create a [headless service](http://kubernetes.io/docs/user-guide/services/#headless-services) and set `spec.clusterIP` to `None`. Selector for this service in my case (`discovery-svc.yaml` in root directory) is
```selector:
        name: seed-node
```

And associated selector in spec in `backend-seed.yaml` is `seed-node`. So when you first deploy discovery service and then backend seed nodes, all pods created from backend-seed.yaml deployment will show up as IPs associated with `discovery-svc.default.svc.cluster.local` domain. That's how each seed node finds all previously registered currently running seed nodes' IPs, same mechanism applies to regular non-seed nodes as well.
