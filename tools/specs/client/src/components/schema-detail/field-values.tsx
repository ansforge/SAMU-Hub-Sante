import { ListIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

// short enum lists stay visible as badges; longer ones would wrap into a
// wall of chips, so they collapse behind a "voir les valeurs" popover
const INLINE_THRESHOLD = 5;

export function FieldValues({ values }: { values: (string | number)[] }) {
  if (!values.length) return null;

  const badges = values.map((value) => (
    <Badge key={value} variant="neutral">
      {value}
    </Badge>
  ));

  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {values.length <= INLINE_THRESHOLD ? (
        badges
      ) : (
        <Popover>
          <PopoverTrigger render={<Button variant="outline" size="xs" />}>
            <ListIcon />
            Voir les {values.length} valeurs
          </PopoverTrigger>
          <PopoverContent className="flex w-auto max-w-sm flex-row flex-wrap gap-1.5">
            {badges}
          </PopoverContent>
        </Popover>
      )}
    </div>
  );
}
