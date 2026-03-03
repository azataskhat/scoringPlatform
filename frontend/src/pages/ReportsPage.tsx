import ReportForm from "../components/Reports/ReportForm";

export default function ReportsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-white">Reports</h2>
        <p className="text-sm text-gray-500 mt-1">
          Generate security assessment reports for a selected time period
        </p>
      </div>
      <ReportForm />
    </div>
  );
}
