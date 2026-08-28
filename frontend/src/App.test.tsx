import { render, screen } from '@testing-library/react';
import App from './App';

test('renders Get started header', () => {
  render(<App />);
  const headerElement = screen.getByText(/Get started/i);
  expect(headerElement).toBeInTheDocument();
});
