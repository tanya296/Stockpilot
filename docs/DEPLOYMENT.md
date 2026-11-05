# Deploying StockPilot to Azure Kubernetes Service (AKS)

Every push to `main` runs this pipeline (`.github/workflows/ci-cd.yml`):

```
git push
  -> Backend: checkout, compile, test (JUnit/Mockito/MockMvc), package (Maven)
  -> Frontend: npm ci, type-check, build
  -> Images: build backend + frontend Docker images, tag with the commit SHA, push to ACR
  -> Deploy: Ansible creates secrets, applies k8s/ manifests with the new tags,
             waits for the rolling update, smoke-tests the public URL
```

| Folder / file | What it is |
| --- | --- |
| `backend/Dockerfile`, `frontend/Dockerfile` | Multi-stage images (Maven -> JRE, Node -> nginx), both non-root |
| `frontend/nginx.conf` | Serves React, forwards `/api` to the backend Service |
| `k8s/` | Namespace, PostgreSQL StatefulSet, API and web Deployments + Services |
| `ansible/deploy.yml` | Secrets, apply manifests, roll out, verify |
| `ansible/rollback.yml` | `kubectl rollout undo` for both Deployments |
| `.github/workflows/ci-cd.yml` | The pipeline |

---

## One-time setup (about 30 minutes)

Commands are for **PowerShell** on Windows. Replace `stockpilotacr<yourname>` with a unique name (lowercase letters and digits only, it becomes `<name>.azurecr.io`).

### 0. Install the tools

```powershell
winget install -e --id Microsoft.AzureCLI     # then open a NEW terminal
az aks install-cli                             # installs kubectl
```

Activate **Azure for Students** at https://azure.microsoft.com/free/students (college email, no card).

### 1. Log in and set names

```powershell
az login
az account show --query "{name:name, id:id}" -o table     # note the subscription id

$RG       = "stockpilot-rg"
$LOCATION = "centralindia"          # if refused, the error lists the regions your subscription allows
$ACR      = "stockpilotacr<yourname>"
$AKS      = "stockpilot-aks"

az provider register --namespace Microsoft.ContainerService
az provider register --namespace Microsoft.ContainerRegistry
```

### 2. Create the resource group, registry and cluster

```powershell
az group create --name $RG --location $LOCATION

# Container registry (Basic tier is the cheapest)
az acr create --resource-group $RG --name $ACR --sku Basic

# Kubernetes cluster: free control plane, ONE small node, allowed to pull from the registry
az aks create `
  --resource-group $RG --name $AKS `
  --tier free --node-count 1 --node-vm-size Standard_B2s `
  --attach-acr $ACR --generate-ssh-keys
```

`aks create` takes 5-10 minutes. If `Standard_B2s` isn't available in your region, use `Standard_B2ms` or `Standard_D2s_v3`.

### 3. Check you can reach the cluster

```powershell
az aks get-credentials --resource-group $RG --name $AKS
kubectl get nodes          # 1 node, STATUS Ready
```

### 4. Give GitHub Actions access to Azure

```powershell
$SUB = az account show --query id -o tsv
az ad sp create-for-rbac --name stockpilot-github --role contributor `
  --scopes /subscriptions/$SUB/resourceGroups/$RG --json-auth
```

Copy the whole JSON it prints. It only has access to this one resource group.
(If your student tenant refuses with "insufficient privileges", ask your instructor or use a personal Azure account.)

### 5. Add secrets and variables in GitHub

GitHub repo -> **Settings -> Secrets and variables -> Actions**

**Secrets** (hidden):

| Name | Value |
| --- | --- |
| `AZURE_CREDENTIALS` | The JSON from step 4 |
| `DB_PASSWORD` | Any strong password, 12+ characters |
| `JWT_SECRET` | Any random string, 32+ characters |

Generate random values in PowerShell: `-join ((48..57)+(65..90)+(97..122) | Get-Random -Count 40 | % {[char]$_})`

**Variables** (visible):

| Name | Value |
| --- | --- |
| `ACR_NAME` | your registry name, e.g. `stockpilotacrtanya` |
| `AKS_RESOURCE_GROUP` | `stockpilot-rg` |
| `AKS_CLUSTER_NAME` | `stockpilot-aks` |

### 6. First deploy

```powershell
git add .
git commit -m "Add Kubernetes CI/CD pipeline"
git push
```

Open **GitHub -> Actions -> CI/CD**. When the Deploy job is green, its summary shows the URL (`http://<public-ip>`). Open it and log in with **Admin** / `Password@123`. The version (`v1.0.0 · <commit>`) is shown under the login form and in the sidebar.

On the very first deploy, one of the two API pods may restart once: both pods start together and race to insert the demo data; the loser restarts and finds the data already there.

---

## Change and redeploy demo (deliverable 4)

1. Edit `backend/src/main/resources/application.yml`:
   ```yaml
   app:
     version: 1.1.0
     release-note: ${RELEASE_NOTE:Second release - deployed by the pipeline}
   ```
2. `git commit -am "Release 1.1.0" && git push`
3. Watch the pipeline run on its own (Actions tab).
4. Watch Kubernetes replace pods one at a time:
   ```powershell
   kubectl get pods -n stockpilot -w
   ```
5. Refresh the site: it now shows `v1.1.0 · <new commit>` and the new note.
6. Show the new image in the registry:
   ```powershell
   az acr repository show-tags --name $ACR --repository stockpilot-api -o table
   ```

---

## Useful commands (for the demo and viva)

```powershell
kubectl get all -n stockpilot                           # everything at once
kubectl get pods -n stockpilot -o wide                  # pods + which node
kubectl get svc stockpilot-web -n stockpilot            # EXTERNAL-IP = the public URL
kubectl logs deploy/stockpilot-api -n stockpilot        # backend logs
kubectl describe pod <pod-name> -n stockpilot           # why a pod isn't starting
kubectl rollout history deploy/stockpilot-api -n stockpilot
kubectl rollout undo deploy/stockpilot-api -n stockpilot        # go back one version
```

Run Ansible yourself (after `az aks get-credentials`; needs WSL or Linux/Mac, as Ansible doesn't run natively on Windows):

```bash
pip install ansible-core kubernetes
ansible-galaxy collection install -r ansible/requirements.yml
cd ansible
DB_PASSWORD=... JWT_SECRET=... ansible-playbook deploy.yml \
  -e api_image=<acr>.azurecr.io/stockpilot-api:<sha> \
  -e web_image=<acr>.azurecr.io/stockpilot-web:<sha> -e git_sha=<sha>
```

---

## Screenshot checklist (deliverable 3)

- [ ] Azure portal: resource group with the AKS cluster and container registry
- [ ] Registry -> Repositories -> `stockpilot-api` and `stockpilot-web` with SHA tags
- [ ] `kubectl get pods -n stockpilot`: all pods `Running`, `READY 1/1`
- [ ] `kubectl get deploy,svc -n stockpilot`: deployments ready, web Service with EXTERNAL-IP
- [ ] The app in the browser at `http://<EXTERNAL-IP>`, version visible
- [ ] GitHub Actions: a green run showing all 4 jobs, and the Deploy job's Ansible output
- [ ] Before/after screenshots for the change demo

---

## Cost and cleanup

With student credits the main cost is the one node (roughly $1/day) plus the Basic registry and the public IP.

```powershell
az aks stop  --resource-group $RG --name $AKS    # pause the cluster when not demoing (stops node billing)
az aks start --resource-group $RG --name $AKS    # resume before the demo (public IP may change)
az group delete --name $RG --yes                 # delete EVERYTHING after the assignment is graded
```
