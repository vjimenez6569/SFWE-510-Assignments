# Running the application
## Pre-requisites
- [Install Postman](https://www.postman.com/downloads/)
- [Install MVN](https://maven.apache.org/download.cgi)
- [Install Docker Desktop](hhttps://docs.docker.com/get-started/get-docker/)

## Start the Application
1. Clone the repository.
2. Open Docker Desktop.
3. Build the application .jar file.
      ```
      mvn package dockerfile:build     
      ```
4. Run the following Docker command:
     ```
     docker compose -f docker/docker-compose.yml up --force-recreate     
     ```
5. To end the container/application, run the following command:
     ```
     docker compose -f docker/docker-compose.yml down    
     ```

## Testing with Postman
1. Open [Postman](https://learning.postman.com/docs/introduction/overview/).
1. Go to your collections and import into your Postman collections. That .har document has a JSON represantion of all of the enpoints in the License web service.
1. Run each request in the collection and check output in the terminal.