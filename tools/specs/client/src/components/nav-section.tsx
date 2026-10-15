import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Input } from "@/components/ui/input";
import {
  SidebarGroup,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
} from "@/components/ui/sidebar";
import { ChevronRightIcon } from "lucide-react";
import { useState, type ReactNode } from "react";

interface NavSectionProps {
  title: string;
  icon: ReactNode;
  defaultOpen?: boolean;
  search: string;
  onSearchChange: (search: string) => void;
  toolbar?: ReactNode;
  error?: Error | null;
  children: ReactNode;
}

export function NavSection({
  title,
  icon,
  defaultOpen = false,
  search,
  onSearchChange,
  toolbar,
  error,
  children,
}: NavSectionProps) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <SidebarGroup>
      <SidebarMenu>
        <Collapsible
          className="group/collapsible"
          open={open}
          onOpenChange={setOpen}
          render={<SidebarMenuItem />}
        >
          <CollapsibleTrigger render={<SidebarMenuButton tooltip={title} />}>
            {icon}
            <span>{title}</span>
            <ChevronRightIcon className="ml-auto transition-transform duration-200 group-data-open/collapsible:rotate-90" />
          </CollapsibleTrigger>
          <CollapsibleContent>
            <div className="flex items-center gap-1">
              <Input
                autoFocus
                type="search"
                placeholder="Rechercher..."
                value={search}
                onChange={(e) => onSearchChange(e.target.value)}
                className="mx-2 my-1 w-[calc(100%-1rem)] group-data-[collapsible=icon]:hidden grow"
              />
              {toolbar}
            </div>
            {error && (
              <p className="px-2 py-1 text-sm text-destructive group-data-[collapsible=icon]:hidden">
                {error.message}
              </p>
            )}
            <SidebarMenuSub>{children}</SidebarMenuSub>
          </CollapsibleContent>
        </Collapsible>
      </SidebarMenu>
    </SidebarGroup>
  );
}
