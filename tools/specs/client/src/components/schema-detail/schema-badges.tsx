import { Badge } from "@/components/ui/badge";

interface SchemaBadges {
  perimeters: string[];
}
export const SchemaBadges = ({ perimeters }: SchemaBadges) => {
  return (
    <div className="flex items-center gap-1">
      {perimeters.map((p) => (
        <Badge key={p} variant="perimeter">
          {p}
        </Badge>
      ))}
    </div>
  );
};
