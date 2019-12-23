# pensieve
How to control data flow rate

## Installation

If you use Maven just add this dependency:

```
    <dependency>
      <groupId>com.fauch.code</groupId>
      <artifactId>pensieve</artifactId>
      <version>1.0</version>
    </dependency>
```

## Controlling flow rate with pensieve

Pensieve use a queue to control data flow rate. On the first side,
data to transfer are push in the queue. On the other side, a driver 
unstack data and delegate their processing to a consumer.

So to transfer data by controlling flow rate, the first thing to do 
is to instantiate the right driver. 
There are currently three drivers implemented:
* `ScheduleAtFixedRate`: with the same period between each unstacking and start of data processing. 
* `ScheduleWithFixedDelay`: with a fixed delay between the end of the first data processing and the start of the second one.
* `TransferRate`: data unstacking and processing according a transfer rate given in bits/s

You can implement your own one by extending `AbsDriver`.

Now create a `FlowController` by calling the `execute` method with following arguments:
* `consumer`: to process data
* `byteFunction`: the function to call to compute the size in bytes processed data.

``` 
    try(FlowController<String> ctrl = new TransferRate(48).execute(p -> process(p), p->p.length())) {
        [...]
    }
```

Open the controller and you can now transfer data using the `transfer` method.

### Simple example

This simple example transfer strings into a list by respecting a transfer rate of 48 bit/s.

```
    final ArrayList<String> packets = new ArrayList<String>();
    try(FlowController<String> ctrl = new TransferRate(48).execute(p -> packets.add(p), p->p.length())) {
        ctrl.open();
        for (int i = 1; i <=2; i++) {
            ctrl.transfer("HELLO ");
            ctrl.transfer("WORLD!");
        }
    }
```

### Transfer data over network by controlling flow rate

Pensieve provides a socket with a flow controller to control the data send rate.
All you have to do is instantiate a socket like this:

```
    try(SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new TransferRate(24))
            .networkInterface("lo")
            .open()) {
        [...]
    }
```

Now, you just have to send packets by calling the `send` method. The packets will be sent with a given rate depending on the driver used.

```
   final String[] words = new String[] {"HELLO WORLD!", "PENSIEVE", "HEDWIG", "HARRY POTTER"};
   try(SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new TransferRate(24))
           .networkInterface("lo")
           .open()) {
       for (String w : words) {
           final byte[] data = w.getBytes();
           socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("233.54.12.235"), 4446));
       }
   }
```

### Combine pensieve with hedwig

By combining pensieve with hedwig you will be able to transfer TFTP files with a given data rate.

```
try (SocketWithControlFlowRate socket = SocketWithControlFlowRate.with(new TransferRate(512)).open()){
    socket.setSoTimeout(10);
    if (action.equals("put")) {
        try(InputStream input = Files.newInputStream(file)) {
            final LocalDateTime start = LocalDateTime.now();
            new TFTP(socket).put(InetAddress.getLocalHost(), 69, input, "file.txt", "octet");
        }
    }
}
```
