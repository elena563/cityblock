import { Field, FieldLabel } from '../../../components/ui/field.tsx'
import { Input } from '../../../components/ui/input.tsx'
import { Button } from '@/components/ui/button.tsx'

export default function RegisterForm({ onRegistered }: { onRegistered: () => void }) {
     return (
      <div className="flex flex-col items-center justify-center bg-card p-6 rounded-lg shadow-lg">
        <h2 className="text-2xl text-foreground">Welcome to</h2>
        <img src="/images/logo-mixed.webp" alt="CityBlock Logo" className="mt-2 w-32 pointer-events-none" />
        <p className="text-left mt-4 w-full">A city-purchasing game for everyone</p>
        <p className="text-left w-full">Register with a username to start playing!</p>
        <div className="flex flex-col md:flex-row items-end justify-center gap-4 mt-4 w-full">
          <Field>
            <FieldLabel htmlFor="name">Username</FieldLabel>
            <Input id="name" name="name" placeholder="Just a nickname.." />
          </Field>
          <Button className="w-full md:w-auto" onClick={onRegistered}>Register</Button>
        </div>
      </div>
  )
}