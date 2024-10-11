Issues
==

Knative scale to zero does not support async - pod will be terminated if there is no request/response traffic

## Solution?
Use knative to trigger a K8s job that starts up, does work, then cleans it's own pod. https://kubernetes.io/docs/concepts/workloads/controllers/job/