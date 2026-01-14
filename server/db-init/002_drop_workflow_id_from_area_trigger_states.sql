-- Fix failing inserts into area_trigger_states by removing an obsolete workflow_id column
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'area_trigger_states'
          AND column_name = 'workflow_id'
    ) THEN
        -- Column was temporarily added during workflow refactor; it is not used by Area polling
        ALTER TABLE public.area_trigger_states DROP COLUMN IF EXISTS workflow_id;
    END IF;
END
$$;

