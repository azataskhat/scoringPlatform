import VulnTable from "../components/Vulnerabilities/VulnTable";

export default function VulnerabilitiesPage() {
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold">Vulnerabilities</h2>
      <VulnTable />
    </div>
  );
}
