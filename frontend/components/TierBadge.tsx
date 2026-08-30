interface TierBadgeProps {
  tier: string;
}

export default function TierBadge({ tier }: TierBadgeProps) {
  return (
    <div
      className="inline-block rounded-full px-9 py-3 font-serif text-xl font-bold uppercase tracking-widest text-white shadow-lg"
      style={{
        background: "linear-gradient(135deg, #F59E0B 0%, #EA580C 50%, #DC2626 100%)",
        boxShadow: "0 4px 12px rgba(234,88,12,0.3)",
      }}
    >
      {tier}
    </div>
  );
}
