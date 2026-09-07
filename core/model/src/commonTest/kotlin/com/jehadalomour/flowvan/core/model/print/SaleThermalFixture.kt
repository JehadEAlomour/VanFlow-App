package com.jehadalomour.flowvan.core.model.print

/**
 * A realistic `GET /invoice-templates/resolve-all?storeNumber=VAN1` body (the `data` of the
 * envelope), shaped exactly as docs/SPEC-print-templates.md §2–§3: one 80 mm SALE template
 * with a header (logo, company name, kind, number/date), a body (items table, totals) and a
 * footer (QR, thank-you line). RETURN is the API's built-in (id null).
 */
object SaleThermalFixture {
    val json = """
{
  "templates": {
    "SALE": {
      "id": "tpl_sale_van",
      "name": "سند بيع — المركبة",
      "documentType": "SALE",
      "paperSize": "THERMAL_80",
      "isDefault": true,
      "branchId": null,
      "layout": {
        "version": 1,
        "layout": {
          "width": 80, "height": null, "unit": "mm",
          "margins": { "top": 3, "right": 4, "bottom": 3, "left": 4 },
          "zones": { "header": { "minHeight": 38 }, "body": { "flex": true }, "footer": { "minHeight": 30 } }
        },
        "elements": [
          { "id": "logo", "type": "LOGO", "zone": "header", "x": 26, "y": 0, "width": 20, "height": 12,
            "props": { "fit": "contain", "align": "center" } },
          { "id": "name", "type": "TEXT", "zone": "header", "x": 0, "y": 13, "width": 72, "height": 8,
            "props": { "content": "{{company.nameAr}}", "fontSize": 14, "fontWeight": "bold", "align": "center",
                       "direction": "rtl", "fontFamily": "arabic" } },
          { "id": "kind", "type": "TEXT", "zone": "header", "x": 0, "y": 22, "width": 72, "height": 6,
            "props": { "content": "{{invoice.kindName}} {{invoice.taxExempt}}", "fontSize": 11, "align": "center", "direction": "rtl" } },
          { "id": "meta", "type": "TEXT", "zone": "header", "x": 0, "y": 29, "width": 72, "height": 9,
            "props": { "content": "رقم: {{invoice.number}}\nالتاريخ: {{invoice.date}} {{invoice.time}}\nالعميل: {{invoice.customer.name}} ({{invoice.customer.number}})",
                       "fontSize": 9, "align": "right", "direction": "rtl", "lineHeight": 1.3 } },
          { "id": "rule1", "type": "DIVIDER", "zone": "header", "x": 0, "y": 37, "width": 72, "height": 1,
            "props": { "style": "dashed" } },
          { "id": "items", "type": "ITEMS_TABLE", "zone": "body", "x": 0, "y": 1, "width": 72, "height": null,
            "props": { "columns": [
              { "key": "name", "labelEn": "Item", "labelAr": "الصنف", "width": 3, "align": "right" },
              { "key": "qty", "labelEn": "Qty", "labelAr": "الكمية", "width": 1, "align": "center" },
              { "key": "price", "labelEn": "Price", "labelAr": "السعر", "width": 1.5, "align": "center" },
              { "key": "taxPct", "labelEn": "Tax", "labelAr": "ضريبة", "width": 1, "align": "center", "hide": true },
              { "key": "total", "labelEn": "Total", "labelAr": "المجموع", "width": 1.5, "align": "left" }
            ] } },
          { "id": "totals", "type": "TOTALS_BLOCK", "zone": "body", "x": 20, "y": 2, "width": 52, "height": null,
            "props": { "rows": [
              { "label": "Subtotal", "labelAr": "المجموع الفرعي", "value": "{{invoice.subtotal}}" },
              { "label": "Discount", "labelAr": "الخصم", "value": "{{invoice.discount}}" },
              { "label": "Tax", "labelAr": "الضريبة", "value": "{{invoice.taxTotal}}" },
              { "label": "Change", "labelAr": "الباقي", "value": "{{invoice.change}}", "hide": true },
              { "label": "Total", "labelAr": "الإجمالي", "value": "{{invoice.total}}", "style": "bold" }
            ] } },
          { "id": "qr", "type": "QR_CODE", "zone": "footer", "x": 26, "y": 1, "width": 20, "height": 20,
            "props": { "data": "{{invoice.qrData}}" } },
          { "id": "thanks", "type": "TEXT", "zone": "footer", "x": 0, "y": 22, "width": 72, "height": 6,
            "props": { "content": "{{company.footerNoteAr}}", "fontSize": 9, "align": "center", "direction": "rtl",
                       "color": "#637181", "someFutureProp": true } },
          { "id": "sp", "type": "SPACER", "zone": "footer", "x": 0, "y": 28, "width": 72, "height": 2, "props": {} }
        ]
      }
    },
    "RETURN": {
      "id": null,
      "name": "built-in",
      "documentType": "RETURN",
      "paperSize": "THERMAL_80",
      "isDefault": false,
      "branchId": null,
      "layout": { "version": 1, "layout": { "width": 80, "height": null, "unit": "mm",
        "margins": { "top": 2, "right": 2, "bottom": 2, "left": 2 },
        "zones": { "header": { "minHeight": 10 }, "body": { "flex": true }, "footer": { "minHeight": 5 } } },
        "elements": [
          { "id": "t", "type": "TEXT", "zone": "header", "x": 0, "y": 0, "width": 76, "height": 6,
            "props": { "content": "{{invoice.kindName}}" } },
          { "id": "future", "type": "SHIFTS_TABLE", "zone": "body", "x": 0, "y": 0, "width": 76, "height": null, "props": {} }
        ] }
    }
  },
  "version": "2026-09-07T10:00:00.000Z",
  "extraTopLevelField": "ignored"
}
""".trimIndent()
}
