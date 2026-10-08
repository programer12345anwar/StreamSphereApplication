import { NavLink as RouterNavLink, NavLinkProps } from "react-router-dom";
import { createElement, forwardRef } from "react";
import { cn } from "@/lib/utils";

interface NavLinkCompatProps extends Omit<NavLinkProps, "className"> {
  className?: string;
  activeClassName?: string;
  pendingClassName?: string;
}

const NavLink = forwardRef<HTMLAnchorElement, NavLinkCompatProps>(
  ({ className, activeClassName, pendingClassName, to, ...props }, ref) => {
    return createElement(RouterNavLink, {
      ref,
      to,
      className: ({ isActive, isPending }) =>
        cn(className, isActive && activeClassName, isPending && pendingClassName),
      ...props,
    });
  },
);

NavLink.displayName = "NavLink";

export { NavLink };
