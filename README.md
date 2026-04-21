# HCL Enterprise Inventory & Supply Chain Management System

A high-performance, real-time solution for managing complex inventory workflows, purchase orders, and supplier relationships with integrated business intelligence.

## 🚀 Key Features

- **Advanced Analytics**: Real-time tracking of revenue trends, inventory turnover rates, and category performance.
- **BI Dashboard**: Deep insights into financial performance (YTD Revenue, Gross Profit) and Supply Chain health (Otif Rate, Lead Times).
- **Professional Reporting**: Integrated PDF export functionality for analytical reports and BI intelligence summaries.
- **Procurement Workflow**: End-to-end Purchase Order management with automated stock reconciliation upon receipt.
- **Supplier Relationship Management**: Supplier performance tracking, risk assessment, and integrated metrics.
- **Role-Based Security**: Advanced access control for Admins, Managers, and specialized roles like Analysts and Procurement officers.

## 🛠 Tech Stack

### Backend
- **Core**: Spring Boot 3.3.5
- **Language**: Java 22 (JDK 22.0.1)
- **Database**: PostgreSQL
- **Security**: Spring Security with JWT (JSON Web Tokens)
- **Persistence**: Spring Data JPA / Hibernate

### Frontend
- **Framework**: Angular 18
- **Styling**: Vanilla CSS with a focus on premium, high-end aesthetics
- **Visualizations**: Ngx-Charts for dynamic data representation
- **Reporting**: jsPDF & jsPDF-AutoTable for professional PDF generation

## ⚙️ Getting Started

### Prerequisites
- JDK 22
- Node.js (v18+) & npm
- PostgreSQL

### Local Setup

**1. Clone the Repository**
```bash
git clone https://github.com/lasikarathore/HCL-LMS-.git
cd HCL-LMS-
```

**2. Backend Setup**
```bash
cd backend
# Configure datasource in src/main/resources/application.properties
mvn spring-boot:run
```

**3. Frontend Setup**
```bash
cd frontend
npm install
npm run start
```

## 📊 Business Intelligence
The system features an "Analytical Intelligence" suite that provides:
- **Financial KPIs**: Automated calculation of COGS, Net Margin, and Gross Profit.
- **Supply Chain Metrics**: Real-time monitoring of Supplier Risk and Lead Time fulfillment.
- **Historical Ledger**: Complete audit trail of all inventory and financial transactions.

## 📝 License
Proprietary / Enterprise Edition
