// hooks/useCreateUser.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { SchemaService } from "@/services/schema-service";

export const useUpdateSchema = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: SchemaService.update,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
};
