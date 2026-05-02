docker compose version

docker compose up -d

Wait for initialization: The initial startup seeds massive vulnerability databases. This takes about 15 to 30 minutes to complete in the background. You can view its progress by running 

docker logs -f dtrack-apiserver

http://localhost:8080.

Username: admin
Password: admin