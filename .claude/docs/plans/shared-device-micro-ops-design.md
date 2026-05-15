# Shared Device Micro-Ops Platform Design

## Problem Statement
The user wants to build a "Shared Device Micro-Ops Platform" (Project #4) which focuses on the operational aspects of IoT devices (vending machines, coffee machines, etc.) beyond just connectivity. This includes inventory management, fault detection, work order lifecycles, route optimization, and business analytics.

## Initial Understanding
- **Focus**: Site, Device, Inventory, Work Orders, Revenue.
- **Infrastructure**: Aliyun server for DB/Middleware (PostgreSQL, Redis, etc.), Local Windows for app code (JDK 21, uv/pnpm).
- **Phases**: 
  1. Site/Device Management + Monitoring.
  2. Inventory + Faults + Work Orders.
  3. Prediction + Analytics + Routing.
  4. AI Assistant.

## Goals
- Efficient operational management of geographically distributed shared devices.
- Minimizing downtime and stockouts.
- Maximizing operational efficiency and revenue.

## Requirements
- **Backend**: Java 21 / Spring Boot 3.x (as per user context).
- **Frontend**: React or Vue (to be confirmed).
- **Design**: High-quality, premium UI/UX inspired by 대厂 (big tech) references.
- **Infrastructure**: SSH tunnel to Aliyun Docker containers.

## Open Questions
- Specific tech stack choices (React vs Vue, DB extensions, etc.).
- MVP scope details (Single device type vs Multi-device platform).
- Inventory management depth.
- UI/UX preference.
