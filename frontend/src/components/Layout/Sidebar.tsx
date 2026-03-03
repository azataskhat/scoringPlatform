import { NavLink } from "react-router-dom";

const links = [
  { to: "/", label: "Dashboard", icon: "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0h4" },
  { to: "/sources", label: "Sources", icon: "M4 7v10c0 2 1 3 3 3h10c2 0 3-1 3-3V7M4 7l8-4 8 4M4 7l8 4m8-4l-8 4" },
  { to: "/map", label: "Map", icon: "M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" },
  { to: "/vulnerabilities", label: "Vulnerabilities", icon: "M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
  { to: "/reports", label: "Reports", icon: "M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" },
];

export default function Sidebar() {
  return (
    <aside className="w-64 bg-gray-900 border-r border-border min-h-screen p-4 flex flex-col">
      <div className="mb-8">
        <h1 className="text-xl font-bold text-primary">IoT Security</h1>
        <p className="text-xs text-gray-500 mt-1">OSINT Scoring Platform</p>
      </div>
      <nav className="flex-1 space-y-1">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end={link.to === "/"}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                isActive
                  ? "bg-primary/20 text-primary font-medium"
                  : "text-gray-400 hover:text-white hover:bg-gray-800"
              }`
            }
          >
            <svg className="w-5 h-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d={link.icon} />
            </svg>
            {link.label}
          </NavLink>
        ))}
      </nav>
      <div className="pt-4 border-t border-border">
        <p className="text-xs text-gray-600 text-center">v1.0.0</p>
      </div>
    </aside>
  );
}
