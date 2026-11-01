import { Field, FieldLabel } from '../../../components/ui/field.tsx'
import { Input } from '../../../components/ui/input.tsx'
import { Button } from '@/components/ui/button.tsx'

export default function RoomSelector() {
     return (
      <div className="flex flex-col items-center justify-center bg-card p-6 rounded-lg shadow-lg max-w-sm">
        <h2 className="text-2xl text-foreground text-center">Choose your playing<br /> environment</h2>
        <p className="text-left mt-4 w-full">Insert a valid room code to join your friends' room</p>
        <div className="flex flex-col md:flex-row items-end justify-center gap-4 mt-4 w-full">
          <Field>
            <FieldLabel htmlFor="name">Room Code</FieldLabel>
            <Input id="name" name="name" />
          </Field>
          <Button className="w-full md:w-auto">Join Room</Button>
        </div>
        <div className="flex w-full items-center gap-3 my-4">
          <span className="flex-1 border-t border-1 border-foreground" aria-hidden="true" />
          <span className="shrink-0">or</span>
          <span className="flex-1 border-t border-1 border-foreground" aria-hidden="true" />
        </div>
        <p className="text-left w-full">Create your own room</p>
        <Button className="w-full mt-4">Create Room</Button>
      </div>
  )
}