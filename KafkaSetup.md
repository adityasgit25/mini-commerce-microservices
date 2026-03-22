# 🚀 Kafka Setup on macOS (KRaft Mode – No ZooKeeper)

This guide explains how to set up and run **Apache Kafka (v4.x)** on macOS using **KRaft mode** (modern architecture without ZooKeeper).

---

## 🧠 Overview

Kafka 4.x no longer uses ZooKeeper.

Instead, it uses **KRaft (Kafka Raft Metadata Mode)**:

* Handles metadata internally
* Simpler setup
* Production-ready
* No external dependency (ZooKeeper ❌)

---

## 📦 Prerequisites

* macOS
* Homebrew installed
* Java (automatically installed with Kafka via Homebrew)

---

## ⚙️ Step 1: Install Kafka

```bash
brew install kafka
```

Verify installation:

```bash
brew list kafka
```

---

## ⚠️ Important Note

Homebrew installs Kafka binaries correctly, but:

* Config files (`server.properties`) are often **ZooKeeper-based (old)**
* Kafka 4.x requires **KRaft config**

👉 So we will create our own config.

---

## 📝 Step 2: Create KRaft Config File

Create a new file:

```bash
nano ~/kraft-server.properties
```

Paste the following:

```properties
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@localhost:9093

listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093
inter.broker.listener.name=PLAINTEXT
controller.listener.names=CONTROLLER

log.dirs=/tmp/kraft-combined-logs

offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1

group.initial.rebalance.delay.ms=0
```

Save and exit.

---

## 🆔 Step 3: Generate Cluster ID

```bash
kafka-storage random-uuid
```

Copy the generated UUID.

---

## 💾 Step 4: Format Storage (First-Time Only)

```bash
kafka-storage format \
  -t <YOUR_UUID_HERE> \
  -c ~/kraft-server.properties
```

---

## 🚀 Step 5: Start Kafka Server

```bash
kafka-server-start ~/kraft-server.properties
```

Kafka should now start successfully 🎉

---

## ✅ Step 6: Verify Kafka is Running

Open a new terminal:

```bash
kafka-topics --list --bootstrap-server localhost:9092
```

If no error appears → Kafka is running.

---

## 📌 Basic Kafka Commands

### Create Topic

```bash
kafka-topics --create \
  --topic order-created \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

### List Topics

```bash
kafka-topics --list --bootstrap-server localhost:9092
```

### Start Producer

```bash
kafka-console-producer \
  --topic order-created \
  --bootstrap-server localhost:9092
```

### Start Consumer

```bash
kafka-console-consumer \
  --topic order-created \
  --from-beginning \
  --bootstrap-server localhost:9092
```

---

## 🧠 Architecture (KRaft Mode)

```
Kafka (Single Node)
 ├── Controller (manages metadata)
 ├── Broker (handles messages)
 └── No ZooKeeper ✅
```

---

## ❌ Common Errors & Fixes

### ❌ `command not found`

👉 Add Kafka to PATH:

```bash
echo 'export PATH="/usr/local/opt/kafka/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

---

### ❌ `NoSuchFileException (server.properties)`

👉 Use correct config or create your own (recommended)

---

### ❌ ZooKeeper commands not found

👉 Expected. Kafka 4.x removed ZooKeeper completely.

---

## 🔥 Best Practices

* Always use **KRaft mode (modern standard)**
* Avoid ZooKeeper (deprecated)
* Use separate configs for dev/staging/prod
* Store logs outside `/tmp` in production

---

## 🚀 Next Steps

* Integrate Kafka with Node.js (`kafkajs`)
* Build event-driven microservices:

    * Order Service → Kafka → Inventory Service → Notification Service
* Implement:

    * Retry logic
    * Dead Letter Queue (DLQ)
    * Idempotent consumers

---

## 💡 Summary

| Feature      | Old (ZooKeeper) | New (KRaft) |
| ------------ | --------------- | ----------- |
| Setup        | Complex         | Simple      |
| Dependencies | External        | None        |
| Performance  | Good            | Better      |
| Future       | Deprecated      | Recommended |

---

## 🎯 You're Ready!

Kafka is now running locally using modern architecture.

👉 Start building real-time, event-driven systems 🚀
