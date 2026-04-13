import { useEffect, useState, useRef } from "react";
import clsx from "clsx";

function useAnimatedCount(target, duration = 1200) {
  const [count, setCount] = useState(0);
  const ref = useRef(null);
  
  useEffect(() => {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const num = typeof target === "string" ? parseFloat(target) : target;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (isNaN(num) || num === 0) { setCount(0); return; }
    
    const startTime = performance.now();
    
    function animate(now) {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / duration, 1);
      // Ease out cubic
      const eased = 1 - Math.pow(1 - progress, 3);
      setCount(Math.floor(eased * num));
      if (progress < 1) {
        ref.current = requestAnimationFrame(animate);
      } else {
        setCount(num);
      }
    }
    ref.current = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(ref.current);
  }, [target, duration]);
  
  return count;
}

export default function StatsCard({ title, value, subtitle, icon: Icon, color = "blue", trend, className }) {
  const numericValue = typeof value === "string" ? parseFloat(value) : value;
  const isNumeric = !isNaN(numericValue) && typeof value === "number";
  const animatedValue = useAnimatedCount(isNumeric ? numericValue : 0);

  const colorStyles = {
    blue: {
      bg: "bg-blue-500/10",
      text: "text-blue-400",
      border: "border-blue-500/20",
      glow: "hover:shadow-[0_0_24px_rgba(59,130,246,0.15)]",
      gradient: "from-blue-500/20 to-transparent",
    },
    indigo: {
      bg: "bg-indigo-500/10",
      text: "text-indigo-400",
      border: "border-indigo-500/20",
      glow: "hover:shadow-[0_0_24px_rgba(99,102,241,0.15)]",
      gradient: "from-indigo-500/20 to-transparent",
    },
    emerald: {
      bg: "bg-emerald-500/10",
      text: "text-emerald-400",
      border: "border-emerald-500/20",
      glow: "hover:shadow-[0_0_24px_rgba(16,185,129,0.15)]",
      gradient: "from-emerald-500/20 to-transparent",
    },
    red: {
      bg: "bg-red-500/10",
      text: "text-red-400",
      border: "border-red-500/20",
      glow: "hover:shadow-[0_0_24px_rgba(239,68,68,0.15)]",
      gradient: "from-red-500/20 to-transparent",
    },
    amber: {
      bg: "bg-amber-500/10",
      text: "text-amber-400",
      border: "border-amber-500/20",
      glow: "hover:shadow-[0_0_24px_rgba(245,158,11,0.15)]",
      gradient: "from-amber-500/20 to-transparent",
    },
    purple: {
      bg: "bg-purple-500/10",
      text: "text-purple-400",
      border: "border-purple-500/20",
      glow: "hover:shadow-[0_0_24px_rgba(168,85,247,0.15)]",
      gradient: "from-purple-500/20 to-transparent",
    },
  };

  const c = colorStyles[color] || colorStyles.blue;

  return (
    <div className={clsx(
      "glass-card p-6 relative overflow-hidden transition-all duration-300 group",
      c.glow,
      className
    )}>
      {/* Subtle gradient overlay */}
      <div className={clsx("absolute top-0 right-0 w-32 h-32 bg-gradient-to-bl rounded-full opacity-40 blur-2xl -translate-y-8 translate-x-8 group-hover:opacity-60 transition-opacity", c.gradient)} />
      
      <div className="flex items-start justify-between relative z-10">
        <div>
          <p className="text-sm font-medium text-zinc-400 mb-1 tracking-wide">{title}</p>
          <h3 className="text-3xl font-bold text-zinc-100 tabular-nums animate-count-up">
            {isNumeric ? animatedValue.toLocaleString() : value}
          </h3>
          {subtitle && <p className="text-xs text-zinc-500 mt-1.5">{subtitle}</p>}
          {trend && (
            <div className={clsx(
              "flex items-center gap-1 mt-2 text-xs font-medium",
              trend > 0 ? "text-emerald-400" : "text-red-400"
            )}>
              <span>{trend > 0 ? "↑" : "↓"}</span>
              <span>{Math.abs(trend)}% from last week</span>
            </div>
          )}
        </div>
        {Icon && (
          <div className={clsx(
            "p-3 rounded-xl border transition-colors duration-300",
            c.bg, c.text, c.border,
            "group-hover:scale-110 transform transition-transform"
          )}>
            <Icon size={22} />
          </div>
        )}
      </div>
    </div>
  );
}
