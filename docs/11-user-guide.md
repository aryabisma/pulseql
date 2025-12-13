# PulseQL-Pulsar Integration - User Guide

**Version**: 1.0  
**Date**: December 13, 2025

## Introduction

This guide explains how to use PulseQL when accessing it from the Pulsar application. The integration provides a seamless query workspace experience with Pulsar's visual style and branding.

## What is PulseQL Workspace Mode?

PulseQL Workspace Mode is a specialized interface designed for Pulsar users that:

- **Matches Pulsar's Look and Feel**: Uses Pulsar's colors, fonts, and styling
- **Simplifies the Interface**: Shows only query-related features
- **Provides Quick Access**: Access PulseQL directly from Pulsar without re-login
- **Maintains Your Permissions**: Respects your Pulsar role and permissions

## Accessing PulseQL from Pulsar

### From Pulsar Application

1. Navigate to any data source or report in Pulsar
2. Click the **"Query Workspace"** or **"Advanced Query"** button
3. PulseQL opens in a new tab or embedded panel
4. You're automatically logged in with your Pulsar credentials

### Direct URL Access

You can also bookmark PulseQL workspace with custom settings:

```
https://pulseql.example.com/workspace?
  mode=pulsar&
  theme=pulsar-light
```

## User Interface Overview

### Workspace Layout

```
┌─────────────────────────────────────────────────┐
│  Pulsar Query Workspace        [Back to Pulsar] │ Header
├──────────────┬──────────────────────────────────┤
│              │                                   │
│  Database    │  SQL Editor                       │
│  Explorer    │  ┌─────────────────────────────┐ │
│              │  │ SELECT * FROM customers     │ │
│  ├─ Sales DB │  │ WHERE country = 'USA'       │ │
│  │  ├─Tables │  │                             │ │
│  │  ├─Views  │  └─────────────────────────────┘ │
│  │  └─...    │                                   │
│              │  [Execute] [Save] [Export]        │
│              │                                   │
│              │  Query Results                    │
│              │  ┌─────────────────────────────┐ │
│              │  │ id │ name      │ country    │ │
│              │  │ 1  │ John Doe  │ USA        │ │
│              │  │ 2  │ Jane Smith│ USA        │ │
│              │  └─────────────────────────────┘ │
└──────────────┴──────────────────────────────────┘
│  Powered by PulseQL                             │ Footer
└─────────────────────────────────────────────────┘
```

### Components

1. **Header Bar**
   - Pulsar branding (logo and title)
   - "Back to Pulsar" button to return to main application

2. **Database Explorer** (Left Panel)
   - Browse available databases and tables
   - View table structures
   - Access views, procedures, and functions
   - Note: Connection management is handled by Pulsar admins

3. **SQL Editor** (Main Panel)
   - Write and edit SQL queries
   - Syntax highlighting
   - Auto-completion
   - Query execution
   - Multiple query tabs

4. **Results Viewer** (Bottom Panel)
   - View query results in a data grid
   - Sort and filter results
   - Export data (if permitted)
   - Copy to clipboard

5. **Footer Bar**
   - Status information
   - Powered by PulseQL branding

## Features

### Writing Queries

1. **Create New Query**
   - Click the "+" tab button or use Ctrl+T
   - Start typing your SQL query
   - Use syntax highlighting and auto-completion

2. **Execute Query**
   - Click the **Execute** button or press Ctrl+Enter
   - View results in the results panel below
   - Check execution time and row count

3. **Save Query**
   - Click the **Save** button
   - Give your query a name
   - Access saved queries from the sidebar

### Working with Results

1. **View Data**
   - Scroll through results
   - Click column headers to sort
   - Use filters to refine data

2. **Export Data** (if permitted)
   - Click the **Export** button
   - Choose format: CSV, JSON, or Excel
   - Save to your computer

3. **Copy Data**
   - Select cells or rows
   - Use Ctrl+C to copy
   - Paste into spreadsheets or documents

### Database Explorer

1. **Browse Objects**
   - Expand database connections
   - Navigate through schemas and tables
   - Click to view table structure

2. **Insert Object Names**
   - Double-click a table name to insert into editor
   - Drag and drop objects into the editor
   - Right-click for context menu

## Themes

PulseQL workspace supports two themes that match Pulsar's visual style:

### Light Theme (Default)
- Clean, bright interface
- Pulsar blue primary color (#1976D2)
- White backgrounds
- High contrast for readability

### Dark Theme
- Reduced eye strain in low light
- Same Pulsar blue accent
- Dark backgrounds
- Optimized contrast

**Switching Themes**:
- Use the theme selector in settings (if available)
- Or request your Pulsar admin to set default theme

## Permissions

Your access in PulseQL is controlled by your Pulsar role:

### Viewer Role
- ✅ View database structure
- ✅ Execute SELECT queries
- ✅ View query results
- ✅ Copy data to clipboard
- ❌ Modify data (INSERT, UPDATE, DELETE)
- ❌ Export data
- ❌ Save queries

### Analyst Role
- ✅ All Viewer permissions
- ✅ Save queries
- ✅ Export data to CSV/Excel
- ✅ Create complex queries
- ❌ Modify data (INSERT, UPDATE, DELETE)
- ❌ Modify database structure

### Developer Role
- ✅ All Analyst permissions
- ✅ Execute data modification queries
- ✅ Create temporary tables
- ❌ Modify permanent database structure
- ❌ Manage users or permissions

### Admin Role
- ✅ All permissions
- ✅ Modify database structure
- ✅ Manage connections (in Pulsar)

Note: Exact permissions depend on your Pulsar configuration.

## Tips and Best Practices

### Query Performance

1. **Limit Results**: Add `LIMIT` clause to large queries
   ```sql
   SELECT * FROM large_table LIMIT 100;
   ```

2. **Use Filters**: Filter data at the database level
   ```sql
   SELECT * FROM orders 
   WHERE order_date >= '2025-01-01';
   ```

3. **Avoid SELECT ***: Specify only needed columns
   ```sql
   SELECT customer_id, name, email 
   FROM customers;
   ```

### Query Organization

1. **Use Comments**: Document your queries
   ```sql
   -- Monthly sales report
   SELECT 
     month,
     SUM(amount) as total_sales
   FROM sales
   GROUP BY month;
   ```

2. **Format Queries**: Use proper indentation
   ```sql
   SELECT 
     c.name,
     COUNT(o.id) as order_count
   FROM customers c
   LEFT JOIN orders o ON c.id = o.customer_id
   GROUP BY c.name
   ORDER BY order_count DESC;
   ```

3. **Save Frequently Used Queries**: Create a library of useful queries

### Data Export

1. **Choose Appropriate Format**:
   - CSV: For spreadsheets and simple analysis
   - JSON: For programmatic processing
   - Excel: For formatted reports

2. **Verify Data Before Export**: Review results before exporting

3. **Be Mindful of Data Size**: Large exports may take time

## Keyboard Shortcuts

| Action | Windows/Linux | macOS |
|--------|---------------|-------|
| Execute Query | Ctrl+Enter | Cmd+Enter |
| New Query Tab | Ctrl+T | Cmd+T |
| Save Query | Ctrl+S | Cmd+S |
| Format SQL | Ctrl+Shift+F | Cmd+Shift+F |
| Toggle Comments | Ctrl+/ | Cmd+/ |
| Find | Ctrl+F | Cmd+F |
| Replace | Ctrl+H | Cmd+H |
| Undo | Ctrl+Z | Cmd+Z |
| Redo | Ctrl+Y | Cmd+Shift+Z |

## Common Tasks

### Task 1: Find Customers by Country

```sql
SELECT 
  customer_id,
  name,
  email,
  country
FROM customers
WHERE country = 'USA'
ORDER BY name;
```

### Task 2: Calculate Monthly Totals

```sql
SELECT 
  DATE_FORMAT(order_date, '%Y-%m') as month,
  COUNT(*) as order_count,
  SUM(total_amount) as total_sales
FROM orders
WHERE order_date >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
GROUP BY DATE_FORMAT(order_date, '%Y-%m')
ORDER BY month DESC;
```

### Task 3: Join Multiple Tables

```sql
SELECT 
  c.name as customer_name,
  o.order_date,
  p.product_name,
  oi.quantity,
  oi.unit_price
FROM customers c
INNER JOIN orders o ON c.id = o.customer_id
INNER JOIN order_items oi ON o.id = oi.order_id
INNER JOIN products p ON oi.product_id = p.id
WHERE o.order_date >= '2025-01-01'
ORDER BY o.order_date DESC, c.name;
```

## Troubleshooting

### Issue: Cannot See Query Workspace Button

**Solution**: 
- Check with your Pulsar admin if you have query access
- Verify you're on a page that supports query workspace
- Try refreshing the Pulsar page

### Issue: Query Takes Too Long

**Solution**:
- Add `LIMIT` clause to reduce result set
- Check if filters are properly indexed
- Consider breaking complex queries into smaller parts
- Contact your database admin if problem persists

### Issue: Cannot Export Data

**Solution**:
- Verify you have export permissions in Pulsar
- Check if file size is too large
- Try exporting fewer rows or columns
- Contact your Pulsar admin for permission increase

### Issue: Lost Query Work

**Solution**:
- Use the "Save" feature frequently
- Enable auto-save in settings (if available)
- Keep a backup of important queries in a text file

### Issue: Back to Pulsar Button Not Working

**Solution**:
- Refresh the PulseQL page
- Close and reopen from Pulsar
- Check browser popup blocker settings
- Contact technical support

## Getting Help

### In-App Help
- Look for the help icon (?) in the interface
- Hover over buttons for tooltips
- Check the documentation link in footer

### Support Channels
- **Pulsar Help Desk**: For permission and access issues
- **IT Support**: For technical problems
- **Database Team**: For query optimization help
- **Training Team**: For SQL learning resources

## Security and Privacy

### Best Practices

1. **Protect Your Credentials**
   - Never share your Pulsar login
   - Log out when done
   - Don't leave workstation unattended

2. **Handle Data Responsibly**
   - Don't export sensitive data unnecessarily
   - Follow company data handling policies
   - Delete exported files when no longer needed

3. **Write Safe Queries**
   - Test modifications on small datasets first
   - Use transactions for multiple changes
   - Double-check WHERE clauses in UPDATE/DELETE

### Data Privacy

- PulseQL respects your Pulsar data access policies
- Audit logs track all query activity
- Exported data follows company retention policies
- Queries are automatically logged for compliance

## Frequently Asked Questions

**Q: Can I access PulseQL without going through Pulsar?**  
A: PulseQL is integrated with Pulsar for security. Direct access may be restricted by your organization.

**Q: Why can't I create new database connections?**  
A: Connections are managed centrally by Pulsar admins to ensure security and compliance.

**Q: How long are my saved queries kept?**  
A: Saved queries are stored indefinitely unless deleted by you or due to retention policies.

**Q: Can I share queries with colleagues?**  
A: This depends on your Pulsar configuration. Check with your admin about query sharing features.

**Q: What databases can I query?**  
A: You can query databases that you have access to in Pulsar, as determined by your role and permissions.

## Appendix A: SQL Quick Reference

### Basic SELECT
```sql
SELECT column1, column2 
FROM table_name 
WHERE condition;
```

### Aggregation
```sql
SELECT column, COUNT(*), SUM(amount)
FROM table_name
GROUP BY column
HAVING COUNT(*) > 1;
```

### Joins
```sql
SELECT t1.col, t2.col
FROM table1 t1
JOIN table2 t2 ON t1.id = t2.fk_id;
```

### Subqueries
```sql
SELECT * FROM table1
WHERE id IN (SELECT fk_id FROM table2);
```

For comprehensive SQL learning, consult your organization's training resources.

## Appendix B: Glossary

- **Schema**: Organizational structure of database objects
- **Query**: SQL statement to retrieve or modify data
- **Result Set**: Data returned by a query
- **Export**: Save query results to a file
- **Workspace**: PulseQL interface for query operations
- **Connection**: Link to a database system
- **Permissions**: Access rights to data and operations

---

**Document Version**: 1.0  
**Last Updated**: December 13, 2025  
**For Support**: Contact your Pulsar administrator or IT help desk
