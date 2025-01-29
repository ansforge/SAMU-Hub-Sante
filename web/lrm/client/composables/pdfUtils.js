import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { ValidationStatus } from '~/composables/messageUtils.js';

const color = {
  black: [0, 0, 0], // Black
  green: [0, 128, 0], // Green
  red: [255, 0, 0], // Red
  orange: [255, 165, 0], // Orange
};

const addText = (pdf, text, x, y, fontSize = 10, textColor = color.black) => {
  pdf.setFontSize(fontSize);
  pdf.setTextColor(...textColor);
  pdf.text(text, x, y);
};

const addSection = (pdf, section, yOffset) => {
  section.forEach((item, index) => {
    addText(
      pdf,
      item.label,
      14,
      yOffset + index * 8,
      item.fontSize,
      item.color
    );
  });

  return yOffset + section.length * 5; // Add 10px padding after section
};

const addTable = (pdf, table, yOffset) => {
  const rows = table.data.map((item) => {
    const { text, color } = getStatusTextAndColor(item.valid);
    return [
      item.path.join('.'),
      { content: text, styles: { textColor: color } },
      item.description,
    ];
  });

  autoTable(pdf, {
    startY: yOffset,
    head: table.headers,
    body: rows,
    columnStyles: table.columnStyles,
  });

  return pdf.previousAutoTable.finalY + 10;
};

const getStatusTextAndColor = (valid) => {
  switch (valid) {
    case ValidationStatus.VALID:
      return { text: 'Valide', color: color.green };
    case ValidationStatus.INVALID:
      return { text: 'Invalide', color: color.red };
    case ValidationStatus.APPROXIMATE:
      return { text: 'Approximatif', color: color.orange };
    default:
      return { text: 'Non renseigné', color: color.black };
  }
};

const generate = (config) => {
  const pdf = new jsPDF({ orientation: 'landscape' });

  let yOffset = 10; // Initial yOffset

  config.sections.forEach((section, index) => {
    yOffset = addSection(pdf, section.items, yOffset) + 20;

    if (section.table) {
      yOffset = addTable(pdf, section.table, yOffset);
    }

    if (section.counts) {
      yOffset = addSection(pdf, section.counts, yOffset);
    }

    if (index < config.sections.length - 1) {
      pdf.addPage();
      yOffset = 10; // Reset yOffset for new page
    }
  });

  pdf.save(config.filename);
};

export {
  generate,
  color,
  addText,
  addSection,
  addTable,
  getStatusTextAndColor,
};
