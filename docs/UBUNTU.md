# Deployment to ubuntu server (e.g. through digital ocean)

All of the following steps are essential.

### Provision
- Provision an ubuntu server machine (can be a cloud server such as digital ocean)
- A machine with 1gb RAM and 30gb hard disk has been sufficient for early tests
- Enable connection to the box via ssh - [how to](https://www.digitalocean.com/community/tutorials/how-to-set-up-ssh-keys--2) 

### Configure with ansible
- Install Ansible
- In file *ops/digital_ocean_box.inventory* replace the IP address with the IP address of your ubuntu server machine
- Create a Twitter developer account and "app" for user authentication in the app
- Create an AWS (Amazon Web Services) account which will be used to store database backups in S3
- Use the *objective8_config_template* found in the */ops* directory and replace the empty strings with your credentials and save it for use in the next step. Take note of the file path. You can find more information about the configuration variables [here](./CONFIG.md).
- Create a */ops/roles/nginx/files/secure/* directory, and copy your SSL certificate and key files there, with the names *objective8.key* and *objective.crt*.

Run Ansible playbook:

  The following command will install necessary packages and configure them (it will take a few minutes).
  It will require choosing a database password and supplying your Amazon S3 credentials for automatically backing up (encrypted) the database to an Amazon S3 bucket: 
  ```
  ansible-playbook ops/digital_ocean_box_playbook.yml -i ops/digital_ocean_box.inventory --extra-vars "CONFIG_FILE_PATH={config file path from the previous step without the curly braces}"
  ```
  
### Deploy application to the server
Run:

  The following will copy the application to the server and start it running as a service on a docker container.
  Once complete you should be able to access the app at your IP address.

  ```
  chmod +x deploy_prod.sh
  REMOTE_USER={username on server} SERVER_IP={IP address of server} ./deploy_prod.sh
  ```
