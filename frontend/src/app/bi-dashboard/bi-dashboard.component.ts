import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ApiService } from '../service/api.service';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Component({
  selector: 'app-bi-dashboard',
  standalone: true,
  imports: [CommonModule, NgxChartsModule],
  templateUrl: './bi-dashboard.component.html',
  styleUrls: ['./bi-dashboard.component.css']
})
export class BiDashboardComponent implements OnInit {
  
  // Financial KPIs
  financialStats = [
    { label: 'YTD Revenue', value: '₹12,45,000', trend: '+15.4%', trendClass: 'trend-up' },
    { label: 'Gross Profit', value: '₹3,54,000', trend: '+8.2%', trendClass: 'trend-up' },
    { label: 'COGS', value: '₹8,91,000', trend: '+12.1%', trendClass: 'trend-down' },
    { label: 'Net Margin', value: '28.4%', trend: '+2.1%', trendClass: 'trend-up' }
  ];

  // Supply Chain Metrics
  supplyChainStats = [
    { label: 'Otif Rate', value: '94.2%', sub: 'On-time In-full' },
    { label: 'Lead Time', value: '4.2 Days', sub: 'Avg. Fulfillment' },
    { label: 'Stock Turnover', value: '6.8x', sub: 'Annualized' },
    { label: 'Supplier Risk', value: 'Low', sub: 'Weighted Average' }
  ];

  // Chart Data
  financialTrendData = [
    {
      name: 'Revenue',
      series: [
        { name: 'Jan', value: 85000 },
        { name: 'Feb', value: 92000 },
        { name: 'Mar', value: 110000 },
        { name: 'Apr', value: 98000 },
        { name: 'May', value: 125000 },
        { name: 'Jun', value: 140000 }
      ]
    },
    {
      name: 'Profit',
      series: [
        { name: 'Jan', value: 25000 },
        { name: 'Feb', value: 28000 },
        { name: 'Mar', value: 35000 },
        { name: 'Apr', value: 31000 },
        { name: 'May', value: 42000 },
        { name: 'Jun', value: 48000 }
      ]
    }
  ];

  inventoryMixData = [
    { name: 'Electronics', value: 450000 },
    { name: 'Home Appliances', value: 320000 },
    { name: 'Furniture', value: 150000 },
    { name: 'Accessories', value: 80000 }
  ];

  // Table Data (Historical Ledger)
  ledgerData: any[] = [];

  // UI State
  loading = true;
  isExporting = false;

  // Chart Config
  colorScheme: any = {
    domain: ['#6366f1', '#10b981', '#f59e0b', '#ef4444']
  };

  constructor(private api: ApiService) { }

  ngOnInit(): void {
    this.loadBiData();
  }

  loadBiData(): void {
    this.loading = true;
    this.api.getBiSummary().subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res?.status === 200 && res.biAnalytics) {
          const bi = res.biAnalytics;
          this.financialStats = bi.financialStats || [];
          this.supplyChainStats = bi.supplyChainStats || [];
          this.financialTrendData = bi.financialTrendData || [];
          this.inventoryMixData = bi.inventoryMixData || [];
          
          this.ledgerData = (bi.ledgerData || []).map((t: any) => ({
            id: 'TX-' + t.id,
            date: new Date(t.createdAt).toLocaleDateString(),
            type: t.transactionType,
            entity: t.product?.name || t.supplier?.name || 'System',
            amount: t.totalPrice,
            status: t.status
          }));
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  exportData(format: 'CSV' | 'PDF') {
    this.isExporting = true;
    
    if (format === 'CSV') {
      this.generateCSV();
    } else {
      this.generatePDF();
    }
    
    setTimeout(() => this.isExporting = false, 1000);
  }

  private generatePDF() {
    const doc = new jsPDF();
    
    // Header
    doc.setFontSize(22);
    doc.setTextColor(30, 41, 59);
    doc.text('IMS BI Intelligence Report', 14, 22);
    
    doc.setFontSize(10);
    doc.setTextColor(100);
    doc.text(`Generated: ${new Date().toLocaleString()}`, 14, 28);
    doc.text('Confidential Intelligence Report', 14, 33);
    
    // 1. Financial Performance
    doc.setFontSize(14);
    doc.setTextColor(30, 41, 59);
    doc.text('Financial Performance (YTD)', 14, 45);
    
    autoTable(doc, {
      startY: 50,
      head: [['Metric', 'Value', 'Trend']],
      body: this.financialStats.map(s => [s.label, s.value, s.trend]),
      theme: 'striped',
      headStyles: { fillColor: [99, 102, 241] }
    });
    
    // 2. Supply Chain Health
    const finalY = (doc as any).lastAutoTable.finalY + 10;
    doc.text('Supply Chain Health', 14, finalY);
    
    autoTable(doc, {
      startY: finalY + 5,
      head: [['Metric', 'Status', 'Benchmark']],
      body: this.supplyChainStats.map(s => [s.label, s.value, s.sub]),
      theme: 'grid',
      headStyles: { fillColor: [16, 185, 129] }
    });
    
    // 3. Historical Ledger
    doc.addPage();
    doc.text('Historical Ledger Transactions', 14, 22);
    
    autoTable(doc, {
      startY: 28,
      head: [['ID', 'Date', 'Type', 'Entity', 'Amount', 'Status']],
      body: this.ledgerData.map(r => [r.id, r.date, r.type, r.entity, 'INR ' + r.amount, r.status]),
      headStyles: { fillColor: [99, 102, 241] }
    });
    
    doc.save(`IMS_BI_Report_${new Date().toISOString().split('T')[0]}.pdf`);
  }

  private generateCSV() {
    let csv = "BI REPORT - ANALYTICAL INTELLIGENCE\n";
    csv += `Generated: ${new Date().toLocaleString()}\n\n`;

    // 1. Financial Stats
    csv += "FINANCIAL PERFORMANCE\n";
    csv += "Metric,Value,Trend\n";
    this.financialStats.forEach(s => csv += `${s.label},"${s.value}",${s.trend}\n`);
    csv += "\n";

    // 2. Supply Chain Stats
    csv += "SUPPLY CHAIN HEALTH\n";
    csv += "Metric,Value,Note\n";
    this.supplyChainStats.forEach(s => csv += `${s.label},"${s.value}",${s.sub}\n`);
    csv += "\n";

    // 3. Historical Ledger
    csv += "HISTORICAL LEDGER\n";
    csv += "Tx ID,Date,Type,Entity,Amount,Status\n";
    this.ledgerData.forEach(r => csv += `${r.id},${r.date},${r.type},"${r.entity}","${r.amount}",${r.status}\n`);

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `IMS_BI_Report_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
