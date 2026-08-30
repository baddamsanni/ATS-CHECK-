import type { RequirementMatch } from "../app/types";

interface RequirementTableProps {
  requirements: RequirementMatch[];
}

const STATUS_CONFIG = {
  met: { label: "Met", color: "#16A34A", bg: "#DCFCE7" },
  partial: { label: "Partial", color: "#D97706", bg: "#FEF3C7" },
  missing: { label: "Missing", color: "#A8A29E", bg: "#F5F5F4" },
};

export default function RequirementTable({ requirements }: RequirementTableProps) {
  const sorted = [...requirements].sort((a, b) => {
    const order = { met: 0, partial: 1, missing: 2 };
    return order[a.status] - order[b.status];
  });

  return (
    <div className="overflow-hidden rounded-xl border bg-white shadow-sm" style={{ borderColor: "#F5E6D8" }}>
      <table className="w-full border-collapse">
        <thead>
          <tr style={{ background: "#FFF7ED" }}>
            <th className="border-b px-4 py-2.5 text-left text-xs font-bold uppercase tracking-wider" style={{ borderColor: "#F5E6D8", color: "#78716C" }}>
              Requirement
            </th>
            <th className="border-b px-4 py-2.5 text-left text-xs font-bold uppercase tracking-wider" style={{ borderColor: "#F5E6D8", color: "#78716C" }}>
              Status
            </th>
            <th className="border-b px-4 py-2.5 text-left text-xs font-bold uppercase tracking-wider" style={{ borderColor: "#F5E6D8", color: "#78716C" }}>
              Evidence
            </th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((req, i) => {
            const cfg = STATUS_CONFIG[req.status] || STATUS_CONFIG.missing;
            return (
              <tr key={i} style={{ background: i % 2 === 1 ? "#FFFBF7" : "#FFFFFF" }}>
                <td className="border-b px-4 py-2.5 text-sm font-medium" style={{ borderColor: "#F5E6D8", color: "#1C1917" }}>
                  {req.requirement}
                </td>
                <td className="border-b px-4 py-2.5" style={{ borderColor: "#F5E6D8" }}>
                  <span
                    className="inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold"
                    style={{ background: cfg.bg, color: cfg.color }}
                  >
                    {cfg.label}
                  </span>
                </td>
                <td
                  className="border-b px-4 py-2.5 text-sm leading-relaxed"
                  style={{
                    borderColor: "#F5E6D8",
                    color: req.status === "missing" ? "#A8A29E" : "#1C1917",
                  }}
                >
                  {req.status === "missing" ? "—" : req.evidence}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
