1. Find your image with ```docker image ls```
2. Follow this tutorial: https://docs.quay.io/solution/getting-started.html
3. Sign in to Quay.io with ```docker login quay.io```
4. Tag the container with ```docker commit CONTAINER-ID quay.io/USERNAME/REPONAME```
5. Push the image to quay.io with ```docker push quay.io/USERNAME/REPONAME```