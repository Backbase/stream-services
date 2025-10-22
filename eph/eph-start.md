# Run eph commands
1. VPN proxy
    ```bash
    export http_proxy=http://webproxy.infra.backbase.cloud:8888
    export https_proxy=http://webproxy.infra.backbase.cloud:8888
    ```
2. Configure access to eph kubernetes env
    * [generate and export kubeconfig](https://github.com/baas-devops-rndwlt/.github-private/blob/main/ONBOARDING.md#generate-kubeconfig-for-eph-runtime)
    * add to /etc/hosts string 127.0.0.1       mssql-server
- add to /etc/hosts string 127.0.0.1       mssql-server
3. Set `eph` config. `link to setup kubeconfig for eph runtime`
    ```bash
    export KUBECONFIG=~/.kube/rndwlt/eph
    ```
3. Create eph runtime
    * Wealth RC
       ```bash
       eph new wealth-rc.yaml
       ```
   3.1 Edit `eph edit <ephemeral-payload-file> <existing-namespace-to-edit> `
   ```bash
   eph edit wealth-rc.yaml ep-romank-test-1 
   ```
4. Env links:
    * Get eph name: `eph ls`.
        * Example: `ep-app-test`
    * Web: `https://app-<ep-name>.eph.rndwlt.azure.backbaseservices.com/web`.
        * Example: https://app-ep-app-test.eph.rndwlt.azure.backbaseservices.com/web
    * ArgoCD: `https://argo.eph.rndwlt.azure.backbaseservices.com/applications?showFavorites=false&proj=&sync=&autoSync=&health=&namespace=&cluster=&labels=`
        * Find a namespace with your eph name
        * Example: https://argo.eph.rndwlt.azure.backbaseservices.com/applications?showFavorites=false&proj=&sync=&autoSync=&health=&namespace=ep-app-test&cluster=&labels=
    * Identity: `https://identity.<ep-name>.eph.rndwlt.azure.backbaseservices.com/auth/`
        * Example: https://identity-ep-app-test.eph.rndwlt.azure.backbaseservices.com/auth/
          4.1. https://app-ep-romank-test.eph.rndwlt.azure.backbaseservices.com/web


# Eph-cli payload
This folder includes payloads for running eph environments using eph-cli (instructions for installing and using eph-cli can be found [here](https://github.com/backbase-rnd/common-rnd-scripts/tree/main/eph-cli) )

**Note:**  Feature of running **BBOM RC** (*wealth-rc* payload) To run BBOM RC we need to override the BBOM repository and tag.

>                my-custom-ephem:
>                  classes:
>                    dnr-bbom-apps-override:
>                      image:
>                        registry: &bbomOverrideRegistry harborStaging
>                        tag: &bbomOverrideTag "2025.04-rc.72"

> But **backbase-identity** has no rc versions on available repository, so it goes with the latest released version from the *main* branch of the topstack (or you can change topstack branch on payload to specific branch with your changes). And when you override registry and tag you also can stuck with some capability’s don't have this image on overrided values so also need to fix manualy

Also very important is the order of putting the **profileSelector**, it goes in order and for example, if you first specify the selector with which you overwrite the BBOM TAG and repository and then use, for example, dnr-ephemeral, the changes will be overwritten by **dnr-ephemeral**,

>              profileSelector:
>                - my-custom-ephem
>                - dnr-ephemeral


that is, for correct operation you need to understand what and how you want to overwrite and put the profileSelector in the correct order.

If you want set specific name you need to add to prefix **ep-** your name (line  number 4 on payload)  example **ep-testenv**
and if you want to setup **TTL** (Time to Live) you need to change it on line **janitor/ttl: &ttl 4h**



