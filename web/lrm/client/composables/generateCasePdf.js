import { generate, color } from './pdfUtils.js'

const generateCasePdf = (testCase, store, getCounts) => {
  const config = {
    filename: 'test-results.pdf',
    sections: [
      {
        items: [
          { label: `Résultats de la recette du cas de test: ${testCase.value.label}`, fontSize: 20, color: color.black },
          { label: `Description: ${testCase.value.description}`, fontSize: 16, color: color.black },
          { label: `Client: ${store.user.clientId}`, fontSize: 16, color: color.black },
          { label: `Target: ${store.user.targetId}`, fontSize: 16, color: color.black },
          { label: `Vhost: ${store.selectedVhost.vhost}`, fontSize: 16, color: color.black },
          { label: `Modèle: ${store.selectedVhost.modelVersion}`, fontSize: 16, color: color.black },
          { label: `Date: ${new Date().toLocaleString()}`, fontSize: 16, color: color.black }
        ]
      },
      ...testCase.value.steps.map((step) => {
        const counts = getCounts(step)
        return {
          items: [
            { label: step.label, fontSize: 14, color: color.black },
            { label: `Description : ${step.description}`, fontSize: 10, color: color.black },
            { label: `Fichier de test : ${step.file}`, fontSize: 10, color: color.black },
            { label: `Modèle : ${step.model}`, fontSize: 10, color: color.black },
            { label: `Type : ${step.type === 'receive' ? 'Réception' : 'Envoie'}`, fontSize: 10, color: color.black }
          ],
          table: {
            headers: [['Path', 'Statut', 'Commentaire']],
            data: step.requiredValues,
            columnStyles: {
              0: { cellWidth: 120 }, // Path column
              1: { cellWidth: 30 }, // Statut column
              2: { cellWidth: 120 } // Commentaire column
            }
          },
          counts: [
            { label: `Nombre de valeurs correctes: ${counts.valid}`, fontSize: 10, color: color.green },
            { label: `Nombre de valeurs approximatives: ${counts.approximate}`, fontSize: 10, color: color.orange },
            { label: `Nombre de valeurs incorrectes: ${counts.invalid}`, fontSize: 10, color: color.red },
            { label: `Nombre de valeurs renseignées: ${counts.total - counts.unreviewed}`, fontSize: 10, color: color.black },
            { label: `Nombre de valeurs non renseignées: ${counts.unreviewed}`, fontSize: 10, color: color.black },
            { label: `Nombre total de valeurs: ${counts.total}`, fontSize: 10, color: color.black }
          ]
        }
      })
    ]
  }
  generate(config)
}
export { generateCasePdf }
