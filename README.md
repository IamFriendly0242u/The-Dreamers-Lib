# The Dreamers Lib

**The Dreamers Lib** is a centralized library and utility framework built for Fabric servers and clients running Minecraft 26.1.2 and Java 25. This library serves as the core foundation for all mods under The Dreamers suite, ensuring shared packet structures, automated configuration lifecycles, and cryptographic utilities are handled efficiently with minimal overhead

## Key Features

* **Unified Network Lifecycle**: Centralized registration handles for custom packet payloads, simplifying secure serverbound and clientbound data streams.
* **Intelligent Config & Migration Engine**: Provides robust properties parsing with automatic backup rotation to preserve administrator settings across updates safely.
* **Cryptographic & Security Helpers**: Optimized internal matrix codecs for fast data encryption and decryption across network channels.
* **Modular API**: Designed to easily extend and inject utilities into multiple independent standalone mods simultaneously.

## Technical Specifications

* **Platform**: Fabric Loader
* **Minecraft Version**: ~26.1.2
* **Java**: 25+

## Setup & Dependency

To use this library in your development environment, add it to your `build.gradle` dependencies:

```groovy
dependencies {
	minecraft "com.mojang:minecraft:\${project.minecraft_version}"
	implementation "net.fabricmc:fabric-loader:\${project.loader_version}"
	implementation "net.fabricmc.fabric-api:fabric-api:\${project.fabric_api_version}"

	modImplementation files("../TheDreamersLib/build/libs/thedreamers_lib-1.0.0.jar")
}

## License

This project is protected under **All Rights Reserved** license. No part of this software may be copied, modified, merged, published, or redistributed in any form without prior written permission.

---
*Built by IamFriendly0242u*