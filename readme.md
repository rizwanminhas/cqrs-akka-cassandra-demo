### To run:

Execute the `HotelDemo.scala`

### To check cassandra from terminal:

```
docker ps

docker exec -it cqrs-akka-cassandra-demo-cassandra-1 cqlsh

select * from akka.messages;
```
