-- ====================================================================
-- EV CHARGEFLOW — SUPABASE POSTGRESQL SCHEMA & SEED DATA
-- Run this script in your Supabase Project: SQL Editor -> New Query -> Run
-- ====================================================================

-- 1. PROFILES (Extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    phone TEXT,
    vehicle_model TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. CHARGERS (Station Hardware Telemetry)
CREATE TABLE IF NOT EXISTS public.chargers (
    id SERIAL PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,
    location TEXT NOT NULL,
    kw NUMERIC(6, 2) NOT NULL,
    status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'RESERVED', 'CHARGING', 'OUT_OF_SERVICE')),
    utilization_pct INT NOT NULL DEFAULT 0,
    current_booking_id BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. BOOKINGS & CHARGING REQUESTS
CREATE TABLE IF NOT EXISTS public.bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    customer_name TEXT NOT NULL,
    charger_id INT REFERENCES public.chargers(id) ON DELETE SET NULL,
    target_kwh NUMERIC(8, 2) NOT NULL,
    delivered_kwh NUMERIC(8, 2) NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'PENDING_ARRIVAL' CHECK (status IN ('PENDING_ARRIVAL', 'CHARGING', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    priority INT NOT NULL DEFAULT 1 CHECK (priority IN (1, 2, 3)), -- 1=Standard, 2=Premium, 3=Emergency
    is_walkin BOOLEAN NOT NULL DEFAULT FALSE,
    price_per_kwh NUMERIC(6, 2) NOT NULL DEFAULT 18.00,
    service_fee NUMERIC(6, 2) NOT NULL DEFAULT 25.00,
    gst_pct NUMERIC(4, 2) NOT NULL DEFAULT 18.00,
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    arrival_deadline TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. WAITING QUEUE (Deterministic Priority Buffer)
CREATE TABLE IF NOT EXISTS public.waiting_queue (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT REFERENCES public.bookings(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    customer_name TEXT NOT NULL,
    priority INT NOT NULL DEFAULT 1 CHECK (priority IN (1, 2, 3)),
    target_kwh NUMERIC(8, 2) NOT NULL,
    queue_position INT NOT NULL DEFAULT 1,
    is_walkin BOOLEAN NOT NULL DEFAULT FALSE,
    status TEXT NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'ALLOCATED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. INVOICES & SETTLED TRANSACTIONS
CREATE TABLE IF NOT EXISTS public.invoices (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT REFERENCES public.bookings(id) ON DELETE SET NULL,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    customer_name TEXT NOT NULL,
    customer_email TEXT,
    charger_code TEXT NOT NULL,
    energy_kwh NUMERIC(8, 2) NOT NULL,
    energy_cost NUMERIC(10, 2) NOT NULL,
    service_fee NUMERIC(10, 2) NOT NULL,
    gst_amount NUMERIC(10, 2) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    payment_status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK (payment_status IN ('COMPLETED', 'PENDING', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. ACTIVITY LOGS & ALERTS
CREATE TABLE IF NOT EXISTS public.activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    alert_type TEXT NOT NULL DEFAULT 'info' CHECK (alert_type IN ('info', 'ok', 'warn', 'error')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- SEED INITIAL CHARGERS IF EMPTY
INSERT INTO public.chargers (code, location, kw, status, utilization_pct)
VALUES 
    ('C-01', 'Bay A · DC fast', 60.00, 'AVAILABLE', 75),
    ('C-02', 'Bay B · DC fast', 60.00, 'AVAILABLE', 68),
    ('C-03', 'Bay C · DC standard', 30.00, 'AVAILABLE', 54),
    ('C-04', 'Bay D · DC standard', 30.00, 'AVAILABLE', 42),
    ('C-05', 'Bay E · AC Level-2', 22.00, 'AVAILABLE', 31),
    ('C-06', 'Bay F · AC Level-2', 22.00, 'AVAILABLE', 20)
ON CONFLICT (code) DO NOTHING;

-- ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chargers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.waiting_queue ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activity_logs ENABLE ROW LEVEL SECURITY;

-- Allow public reads and writes for station demo / anon keys
CREATE POLICY "Allow public read on profiles" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "Allow public insert on profiles" ON public.profiles FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on profiles" ON public.profiles FOR UPDATE USING (true);

CREATE POLICY "Allow public read on chargers" ON public.chargers FOR SELECT USING (true);
CREATE POLICY "Allow public update on chargers" ON public.chargers FOR UPDATE USING (true);
CREATE POLICY "Allow public insert on chargers" ON public.chargers FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow public all on bookings" ON public.bookings FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all on waiting_queue" ON public.waiting_queue FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all on invoices" ON public.invoices FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public all on activity_logs" ON public.activity_logs FOR ALL USING (true) WITH CHECK (true);
