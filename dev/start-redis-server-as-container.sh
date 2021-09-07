docker run --name redis-server-batch-importer -p 6380:6379 -d redis
#docker run -it --name my-redis-cli --link my-redis-container:redis --rm redis redis-cli -h redis -p 6379
