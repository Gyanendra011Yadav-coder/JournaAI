"use client";

import { useRouter } from "next/navigation";

interface LogoutButtonProps {
  className?: string;
  label?: string;
}

export function LogoutButton({ className, label = "Logout" }: LogoutButtonProps) {
  const router = useRouter();

  const handleLogout = () => {
    localStorage.removeItem("token");
    router.push("/login");
  };

  return (
    <button
      type="button"
      onClick={handleLogout}
      className={className}
    >
      {label}
    </button>
  );
}
