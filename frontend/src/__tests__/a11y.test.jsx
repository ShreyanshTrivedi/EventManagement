import React from 'react'
import { render } from '@testing-library/react'
import { axe, toHaveNoViolations } from 'jest-axe'
import StyleGuide from '../views/StyleGuide'

expect.extend(toHaveNoViolations)

describe('Accessibility snapshots (axe)', () => {
  it('StyleGuide should have no WCAG violations', async () => {
    const { container } = render(<StyleGuide />)
    const results = await axe(container)
    expect(results).toHaveNoViolations()
  })
})
