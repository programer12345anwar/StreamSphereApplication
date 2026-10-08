import React from "react";
import { APP_NAME } from "@/lib/branding";
import { cn } from "@/lib/utils";

interface BrandLogoProps {
  className?: string;
  markClassName?: string;
  iconClassName?: string;
  labelClassName?: string;
}

export function BrandMark({ className, iconClassName }: Pick<BrandLogoProps, "className" | "iconClassName">) {
  return (
    <span className={cn("flex h-7 w-7 items-center justify-center rounded-lg bg-primary", className)}>
      <svg viewBox="0 0 24 24" aria-hidden="true" className={cn("h-4 w-4 fill-primary-foreground", iconClassName)}>
        <path d="M10 8l6 4-6 4V8z" />
      </svg>
    </span>
  );
}

export function BrandLogo({ className, markClassName, iconClassName, labelClassName }: BrandLogoProps) {
  return (
    <span className={cn("flex items-center gap-2", className)}>
      <BrandMark className={markClassName} iconClassName={iconClassName} />
      <span className={cn("text-lg font-semibold tracking-tight text-foreground", labelClassName)}>{APP_NAME}</span>
    </span>
  );
}
