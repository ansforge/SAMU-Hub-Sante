import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Filter } from "lucide-react";

export function PerimeterSelect({
  perimeters,
  selectedPerimeters,
  toggleFilter,
  className,
}: {
  perimeters: string[];
  selectedPerimeters: string[];
  toggleFilter: (value: string, checked: boolean) => void;
  className?: string;
}) {
  return (
    <div className={className}>
      <DropdownMenu>
        <DropdownMenuTrigger
          render={
            <Button variant="outline" className={"relative"}>
              <Filter />
              {selectedPerimeters.length > 0 && (
                <span className="absolute -top-2 -right-2 bg-primary flex items-center justify-center rounded-full h-5 w-5 text-white text-xs">
                  {selectedPerimeters.length}
                </span>
              )}
            </Button>
          }
        />
        <DropdownMenuContent className="w-40">
          <DropdownMenuGroup>
            {perimeters.map((item) => (
              <DropdownMenuCheckboxItem
                key={item}
                checked={selectedPerimeters?.includes(item)}
                onCheckedChange={(checked: boolean) =>
                  toggleFilter(item, checked)
                }
              >
                {item}
              </DropdownMenuCheckboxItem>
            ))}
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
