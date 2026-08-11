import { useRefs } from "@/hooks/use-refs";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { getRouteApi, useNavigate } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Input } from "./ui/input";
import { ChevronDown, GitPullRequest } from "lucide-react";

const rootRouteApi = getRouteApi("__root__");

const RefSelector = () => {
  const { data, isLoading } = useRefs();
  const { ref } = rootRouteApi.useSearch();
  const [filter, setFilter] = useState<string>("");

  const navigate = useNavigate();

  const selectRef = (nextRef: string) =>
    navigate({
      to: ".",
      search: (prev) => ({ ...prev, ref: nextRef }),
      replace: true,
    });

  const filteredTags = useMemo(
    () =>
      data?.tags.filter((tag) =>
        tag.toLocaleLowerCase().includes(filter.toLocaleLowerCase()),
      ),
    [filter, data],
  );

  const filteredBranches = useMemo(
    () =>
      data?.branches.filter((branch) =>
        branch.toLocaleLowerCase().includes(filter.toLocaleLowerCase()),
      ),
    [filter, data],
  );

  return (
    <div>
      <Popover>
        <PopoverTrigger
          render={
            <Button variant="outline">
              <GitPullRequest />
              {ref}
              <ChevronDown />
            </Button>
          }
        />
        <PopoverContent
          align="start"
          className="w-80 flex flex-col p-0! max-h-80 overflow-hidden"
        >
          <Tabs
            defaultValue="branches"
            className="flex flex-col w-full grow min-h-0"
          >
            <TabsList className={"w-full h-8"}>
              <TabsTrigger value="branches">
                Branches (
                {isLoading ? (
                  <Skeleton className="inline-block h-3 w-4 align-middle" />
                ) : (
                  data?.branches.length
                )}
                )
              </TabsTrigger>
              <TabsTrigger value="tags">
                Tags (
                {isLoading ? (
                  <Skeleton className="inline-block h-3 w-4 align-middle" />
                ) : (
                  data?.tags.length
                )}
                )
              </TabsTrigger>
            </TabsList>
            <div className="p-1 border-b">
              <Input
                className="h-7"
                placeholder="Rechercher..."
                autoFocus
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
              />
            </div>
            <div
              className={"p-1 grow min-h-0 overflow-y-auto overscroll-contain"}
            >
              <TabsContent value="branches">
                {isLoading && (
                  <ul className="flex flex-col gap-1">
                    {Array.from({ length: 6 }).map((_, i) => (
                      <li key={i} className="px-1 py-1">
                        <Skeleton className="h-4 w-full" />
                      </li>
                    ))}
                  </ul>
                )}
                <ul>
                  {filteredBranches?.map((branch) => (
                    <li key={branch}>
                      <button
                        type="button"
                        className="w-full truncate text-left px-1 py-1 rounded hover:bg-accent aria-selected:font-semibold"
                        aria-selected={branch === ref}
                        onClick={() => selectRef(branch)}
                      >
                        {branch}
                      </button>
                    </li>
                  ))}
                  {filteredBranches?.length === 0 && (
                    <div className="p-1 flex flex-col items-center justify-center gap-2">
                      <span>Aucune branche trouvée pour "{filter}"</span>
                      <Button
                        size={"sm"}
                        onClick={() => setFilter("")}
                        variant={"ghost"}
                      >
                        Effacer la recherche
                      </Button>
                    </div>
                  )}
                </ul>
              </TabsContent>
              <TabsContent value="tags">
                {isLoading && (
                  <ul className="flex flex-col gap-1">
                    {Array.from({ length: 6 }).map((_, i) => (
                      <li key={i} className="px-1 py-1">
                        <Skeleton className="h-4 w-full" />
                      </li>
                    ))}
                  </ul>
                )}
                <ul>
                  {filteredTags?.map((tag) => (
                    <li key={tag}>
                      <button
                        type="button"
                        className="w-full truncate text-left px-1 py-1 rounded hover:bg-accent aria-selected:font-semibold"
                        aria-selected={tag === ref}
                        onClick={() => selectRef(tag)}
                      >
                        {tag}
                      </button>
                    </li>
                  ))}
                </ul>
                {filteredTags?.length === 0 && (
                  <div className="p-1 flex flex-col items-center justify-center gap-2">
                    <span>Aucun tag trouvé pour "{filter}"</span>
                    <Button
                      size={"sm"}
                      onClick={() => setFilter("")}
                      variant={"ghost"}
                    >
                      Effacer la recherche
                    </Button>
                  </div>
                )}
              </TabsContent>
            </div>
          </Tabs>
        </PopoverContent>
      </Popover>
    </div>
  );
};

export default RefSelector;
