-- Create a function to send NOTIFY on outbox INSERT
CREATE OR REPLACE FUNCTION notify_outbox_insert()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('outbox_insert', NEW.id::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop existing trigger if it exists to avoid conflicts
DROP TRIGGER IF EXISTS outbox_insert_trigger ON outbox;

-- Create a trigger to call the function on INSERT
CREATE TRIGGER outbox_insert_trigger
AFTER INSERT ON outbox
FOR EACH ROW
EXECUTE FUNCTION notify_outbox_insert();

-- Create index on created_at
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox (created_at);